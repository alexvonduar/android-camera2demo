package tau.camera2demo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.Inflater;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.RelativeLayout;

public class Camera2Demo extends Activity implements
        TextureView.SurfaceTextureListener {

    private CameraDevice mCamera;
    private String mCameraID = "0";

    private AutoFitTextureView mPreviewView;
    private AutoFitSurfaceView mPreviewSurface;
    //private TextureView mDrawView;
    private Size mPreviewSize;
    private Size mImageSize;
    private CaptureRequest.Builder mPreviewBuilder;
    private ImageReader mImageReader;

    private Handler mHandler;
    private HandlerThread mThreadHandler;

    // size of images captured in ImageReader Callback
    private int mImageWidth = 1920; //1920
    private int mImageHeight = 1080; //1080

    // Log tag
    private static final String TAG = "Camera2Demo";


    //surface
    private Surface surface;
    //private Surface drawsurface;
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);

        initView();
        initLooper();

        //RelativeLayout layout = (RelativeLayout)findViewById(R.id.relative);
        mPreviewSurface = (AutoFitSurfaceView)findViewById(R.id.drawview);
        mPreviewSurface.setZOrderOnTop(true);
        mPreviewSurface.getHolder().setFormat(PixelFormat.RGBA_8888);
        //RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        //params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        //params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        //params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        //params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        //layout.addView(mPreviewSurface, params);
    }

    // subroutine to run camera
    private void initLooper() {
        mThreadHandler = new HandlerThread("CAMERA2");
        mThreadHandler.start();
        mHandler = new Handler(mThreadHandler.getLooper());
    }

    /*
     *  step 2: using TextureView to display PREVIEW
     */
    private void initView() {
        mPreviewView = (AutoFitTextureView) findViewById(R.id.textureview);
        mPreviewView.setSurfaceTextureListener(this);
        //mDrawView = (TextureView) findViewById(R.id.drawtexture);
        //mDrawView.setSurfaceTextureListener(this);
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            //new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_CAMERA_PERMISSION: {
                //If request is cancelled, the result arrays are empty.
                if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    //permission was granted, do the contacts-related task you want to do.
                } else {
                    //permission was denied, disable the functionality that depends on thhis permission.
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                }
            }
            //other 'case' lines to check for other permissions this app might request
        }
    }


    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }


    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }


    /*
     *  step 3: to set features of camera and open camera
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                          int height) {
        try {
            // to get the manager of all cameras
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            // to get features of the selected camera
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(mCameraID);

            // We don't use a front facing camera in this sample.
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                Log.e("", "camera facing error");
            }

            // to get stream configuration from features
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // For still image captures, we use the largest available size.
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());

            // Find out if we need to swap dimension to get the preview size relative to sensor
            // coordinate.
            int displayRotation = this.getWindowManager().getDefaultDisplay().getRotation();
            //noinspection ConstantConditions
            int mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            boolean swappedDimensions = false;
            switch (displayRotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                    if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                        swappedDimensions = true;
                    }
                    break;
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                        swappedDimensions = true;
                    }
                    break;
                default:
                    Log.e(TAG, "Display rotation is invalid: " + displayRotation);
            }

            //Point displaySize = new Point();
            //this.getWindowManager().getDefaultDisplay().getSize(displaySize);
            int rotatedPreviewWidth = width;
            int rotatedPreviewHeight = height;
            int maxPreviewWidth = mPreviewView.getWidth();
            int maxPreviewHeight = mPreviewView.getHeight();

            if (swappedDimensions) {
                rotatedPreviewWidth = height;
                rotatedPreviewHeight = width;
                maxPreviewWidth = mPreviewView.getHeight();
                maxPreviewHeight = mPreviewView.getWidth();
            }

            int MAX_PREVIEW_WIDTH = 1920;
            int MAX_PREVIEW_HEIGHT = 1080;
            int MAX_IMAGE_WIDTH = 320;
            int MAX_IMAGE_HEIGHT = 240;


            if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                maxPreviewWidth = MAX_PREVIEW_WIDTH;
            }

            if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                maxPreviewHeight = MAX_PREVIEW_HEIGHT;
            }


            // to get the size that the camera supports
            //mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
            // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
            // garbage capture data.
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                    maxPreviewHeight, largest);

            mImageSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    rotatedPreviewWidth, rotatedPreviewHeight, MAX_IMAGE_WIDTH,
                    MAX_IMAGE_HEIGHT, largest);

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mPreviewView.setAspectRatio(
                        mPreviewSize.getWidth(), mPreviewSize.getHeight());
                mPreviewSurface.setAspectRatio(
                        mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mPreviewView.setAspectRatio(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
                mPreviewSurface.setAspectRatio(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }

            // open camera
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                requestCameraPermission();
                return;
            }
            cameraManager.openCamera(mCameraID, mCameraDeviceStateCallback, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                                            int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    // note that the following method will be called every time a frame's ready
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            try {
                mCamera = camera;
                startPreview(mCamera);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };

    /*
     *  step 4: to start PREVIEW
     */
    private void startPreview(CameraDevice camera) throws CameraAccessException {
        SurfaceTexture texture = mPreviewView.getSurfaceTexture();

        // to set PREVIEW size
        texture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
        surface = new Surface(texture);
        try {
            // to set request for PREVIEW
            mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        //SurfaceTexture drawtexture = mDrawView.getSurfaceTexture();
        //drawtexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        //drawsurface = new Surface(drawtexture);

        // to set the format of captured images and the maximum number of images that can be accessed in mImageReader
        mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.YUV_420_888, 2);

        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);

        // the first added target surface is for camera PREVIEW display
        // the second added target mImageReader.getSurface() is for ImageReader Callback where we can access EACH frame
        mPreviewBuilder.addTarget(surface);
        mPreviewBuilder.addTarget(mImageReader.getSurface());

        //output Surface
        List<Surface> outputSurfaces = new ArrayList<>();
        outputSurfaces.add(surface);
        outputSurfaces.add(mImageReader.getSurface());

        /*camera.createCaptureSession(
                Arrays.asList(surface, mImageReader.getSurface()),
                mSessionStateCallback, mHandler);
                */
        camera.createCaptureSession(outputSurfaces, mSessionStateCallback, mHandler);
    }


    private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                updatePreview(session);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };

    private void updatePreview(CameraCaptureSession session)
            throws CameraAccessException {
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);

        session.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);
    }


    /*
     *  step 5: to implement listener and access each frame
     */
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        /*
         *  The following method will be called every time an image is ready
         *  be sure to use method acquireNextImage() and then close(), otherwise, the display may STOP
         */
        @Override
        public void onImageAvailable(ImageReader reader) {
            // get the newest frame
            Image image = reader.acquireNextImage();

            if (image == null) {
                return;
            }

            if (false && image.getFormat() == ImageFormat.YUV_420_888) {
                int[] strides = {0, 0, 0};
                // HERE to call jni method
                Image.Plane planes[] = image.getPlanes();

                boolean forground = JNIUtils.ForgroundDetect(
                        planes[0].getBuffer(),
                        planes[1].getBuffer(),
                        planes[2].getBuffer(),
                        image.getWidth(),
                        image.getHeight(),
                        planes[0].getRowStride(),
                        planes[1].getRowStride(),
                        planes[1].getPixelStride(),
                        mPreviewSurface.getHolder().getSurface(),
                        256, 256, 0.5f, true);
                Log.e("", "take picture " + forground);
            } else {
                Log.e("", "can't support image format " + image.getFormat());
            }

            image.close();
        }
    };


    protected void onPause() {
        if (null != mCamera) {
            mCamera.close();
            mCamera = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
        super.onPause();
    }
}