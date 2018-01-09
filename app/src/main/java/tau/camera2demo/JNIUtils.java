package tau.camera2demo;

import android.media.Image;
import android.view.Surface;

import java.nio.ByteBuffer;


public class JNIUtils {
    // TAG for JNIUtils class
    private static final String TAG = "JNIUtils";

    // Load native library.
    static {
        System.loadLibrary("opencv_objdetect");
        System.loadLibrary("opencv_core");
        System.loadLibrary("opencv_video");
        System.loadLibrary("opencv_imgproc");
        System.loadLibrary("native-lib");
    }

    public static native boolean ForgroundDetect(ByteBuffer y,
                                              ByteBuffer u,
                                              ByteBuffer v,
                                              int width,
                                              int height,
                                              int y_stride,
                                              int uv_stride,
                                              int uv_pstride,
                                              Surface surface,
                                              int boarder_x,
                                              int boarder_y,
                                              float threshold,
                                              boolean debug);
}
