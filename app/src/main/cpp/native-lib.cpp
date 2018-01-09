#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/bitmap.h>

#include "opencv2/core.hpp"
#include <opencv2/core/utility.hpp>
#include "opencv2/imgproc.hpp"
#include "opencv2/video/background_segm.hpp"
#include "opencv2/videoio.hpp"
#include "opencv2/highgui.hpp"
//#include <stdio.h>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "Camera2Demo", __VA_ARGS__)

bool bgfg_detect(uint8_t * y,
                 uint8_t * u,
                 uint8_t * v,
                 const int& width,
                 const int& height,
                 const int& y_stride,
                 const int& uv_stride,
                 const int& uv_pstride,
                 uint8_t * dst,
                 const int& dst_width,
                 const int& dst_height,
                 const int& dst_stride,
                 const int& boarder_x,
                 const int& boarder_y,
                 const float& threshold,
                 const bool& debug);

//convert Y Plane from YUV_420_888 to RGBA and display
extern "C" {
JNIEXPORT jboolean JNICALL Java_tau_camera2demo_JNIUtils_ForgroundDetect(
        JNIEnv *env,
        jobject obj,
        jobject yBuffer,
        jobject uBuffer,
        jobject vBuffer,
        jint width,
        jint height,
        jint y_stride,
        jint uv_stride,
        jint uv_pstride,
        jobject surface,
        jint boarder_x,
        jint boarder_y,
        jfloat threshold,
        jboolean debug) {

    uint8_t * y = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(yBuffer));
    uint8_t * u = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(uBuffer));
    uint8_t * v = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(vBuffer));

    ANativeWindow *window = NULL;
    ANativeWindow_Buffer buffer;
    if (debug) {
        window = ANativeWindow_fromSurface(env, surface);
        ANativeWindow_acquire(window);

        //set output size and format
        //only 3 formats are available:
        //WINDOW_FORMAT_RGBA_8888(DEFAULT), WINDOW_FORMAT_RGBX_8888, WINDOW_FORMAT_RGB_565
        ANativeWindow_setBuffersGeometry(window, 0, 0, WINDOW_FORMAT_RGBA_8888);
        if (int32_t err = ANativeWindow_lock(window, &buffer, NULL)) {
            LOGE("ANativeWindow_lock failed with error code: %d\n", err);
            ANativeWindow_release(window);
        }
    }

    //to display grayscale, first convert the Y plane from YUV_420_888 to RGBA
    //ANativeWindow_Buffer buffer;
    LOGE("surface[%dx%d] src[%dx%d]\n", buffer.width, buffer.height, width, height);
    uint8_t * outPtr = reinterpret_cast<uint8_t *>(buffer.bits);

    bool result = bgfg_detect(y, u, v, width, height, y_stride, uv_stride, uv_pstride, outPtr, buffer.width, buffer.height, buffer.stride, boarder_x, boarder_y, threshold, debug);

    if (debug) {
        ANativeWindow_unlockAndPost(window);
        ANativeWindow_release(window);
    }

    return result;
}

}

static cv::Ptr<cv::BackgroundSubtractor> bg_model = cv::createBackgroundSubtractorMOG2().dynamicCast<cv::BackgroundSubtractor>();
static bool skip = false;
static bool result = false;

bool bgfg_detect(uint8_t * y,
                 uint8_t * u,
                 uint8_t * v,
                 const int& width,
                 const int& height,
                 const int& y_stride,
                 const int& uv_stride,
                 const int& uv_pstride,
                 uint8_t * dst,
                 const int& dst_width,
                 const int& dst_height,
                 const int& dst_stride,
                 const int& boarder_x,
                 const int& boarder_y,
                 const float& threshold,
                 const bool& debug)
{
    //if (bg_model == NULL) {
    //    bg_model = cv::createBackgroundSubtractorMOG2().dynamicCast<cv::BackgroundSubtractor>();
    //}

    cv::Mat fgmask;//, fgimg;

    if (debug) {
        LOGE("y %p u %p v %p %d %d %d %d", y, u, v, u - y, v - u, boarder_x, boarder_y);
        LOGE("%dx%d stride %d %d", width, height, y_stride, uv_stride);
    }

    //cv::Mat img0(height, width, CV_8UC1, (uint8_t *)y);
    //cv::Mat img;
    //cv::cvtColor(img0, img, CV_YUV2BGR_NV21);
    cv::Mat img0(height + height / 2, width, CV_8UC1, y);
    cv::Mat img;
    cv::cvtColor(img0, img, CV_YUV2BGR_NV21);

    //float src_scale = (float)320.0 / (float)width;
    float dst_scale = (float)width / (float)dst_height;

    if (skip) {
        skip = false;
        return result;
    } else {
        skip = true;
    }

    //cv::resize(img, img, cv::Size(320, height * src_scale), cv::INTER_LINEAR);

    //if( fgimg.empty() )
    //fgimg.create(img.size(), img.type());

    //update the model
    //LOGE("%s %s %d", __FILE__, __FUNCTION__, __LINE__);
    bg_model->apply(img, fgmask, -1);

    //fgimg = cv::Scalar::all(0);
    //img.copyTo(fgimg, fgmask);

    //cv::Mat bgimg;
    //bg_model->getBackgroundImage(bgimg);

    if (debug) {
        LOGE("fgmask %dx%d dst %dx%d[%d]", fgmask.size().width, fgmask.size().height, dst_width,
             dst_height, dst_stride);
    }
    int dst_bpp = 4;

    int num_total = 0;
    int num_forground = 0;
    for (size_t y = 0; y < dst_height; y++)
    {
        uint8_t * dstPtr = dst + y * dst_stride * 4;
        for (size_t x = 0; x < dst_width; x++)
        {

            if ((y > boarder_y && y < (dst_height - boarder_y)) && (x > boarder_x && x < (dst_width - boarder_x))) {
                int _y = std::floor((dst_height - 1 - y) * dst_scale);
                int _x = std::floor((dst_width - 1 - x) * dst_scale);
                if (fgmask.at<uint8_t>(_x, _y)) {
                    //if (y > 10 && y < 20) {
                    //for grayscale output, just duplicate the Y channel into R, G, B channels
                    *dstPtr++ = 255; //R
                    *dstPtr++ = 0; //G
                    *dstPtr++ = 0; //B
                    *dstPtr++ = 200; // gamma for RGBA_8888
                    //++rowPtr;
                    num_forground += 1;
                } else {
                    *dstPtr++ = 0; //R
                    *dstPtr++ = 0; //G
                    *dstPtr++ = 0; //B
                    *dstPtr++ = 0; // gamma for RGBA_8888
                }
                num_total += 1;
            } else {
                dstPtr += 4;
            }
        }
    }
    LOGE("num [%d:%d]", num_forground, num_total * threshold);
    result = num_forground > (num_total * threshold) ? true : false;
    return result;
}
