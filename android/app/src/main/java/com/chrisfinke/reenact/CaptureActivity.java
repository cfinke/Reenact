package com.chrisfinke.reenact;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CaptureActivity extends ReenactActivity {
    private Uri originalPhotoUri;

    public String orientation = "portrait";

    private Camera mCamera;
    private CameraPreview mPreview;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        Intent intent = getIntent();
        originalPhotoUri = intent.getParcelableExtra(ORIGINAL_PHOTO_PATH);

        log("Received original photo URI: " + originalPhotoUri.toString());

        int[] originalImageDimensions = getImageDimensions(originalPhotoUri);

        int imageHeight = originalImageDimensions[1];
        int imageWidth = originalImageDimensions[0];

        log("Original image dimensions: " + imageWidth + "x" + imageHeight);

        if ( imageWidth > imageHeight ) {
            orientation = "landscape";
        }

        if ( orientation.equals("portrait") ) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        if (!startCamera()) {
            return;
        }

        ImageView switchButton = (ImageView) findViewById(R.id.switch_camera);

        if (Camera.getNumberOfCameras() == 1) {
            log("Only one camera. Hiding switch button.");
            switchButton.setVisibility(View.INVISIBLE);
        }

        // Add a listener to the Capture button
        ImageButton captureButton = (ImageButton) findViewById(R.id.capture_button);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );

        flipViewForRTL(R.id.back_button);
    }

    public void goBack() {
        super.onBackPressed();

        log("Going back.");

        finish();
    }

    public void goBack(final View view) {
        goBack();
    }

    @Override
    public void onSwipeRight() {
        super.onSwipeRight();
        goBack();
    }

    @Override
    protected void onPause() {
        super.onPause();

        log("onPause");

        releaseCamera();
        clearOriginalPhoto();
    }

    private void initializeOriginalPhoto(){
        ImageView imageView = (ImageView) findViewById(R.id.original_image);
        fadeOriginalImageInAndOut(imageView);

        // The image will be placed inside the view after the camera is instantiated, since
        // it may have to be modified based on the active camera.
    }

    private void clearOriginalPhoto(){
        ImageView imageView = (ImageView) findViewById(R.id.original_image);
        imageView.setImageBitmap(null);
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        startCamera();
    }

    protected boolean startCamera() {
        // Create an instance of Camera

        if (mCamera != null){
            return true;
        }

        mCamera = getCameraInstance();

        if (mCamera == null){
            fatalAlert(R.string.error_no_camera).show();

            return false;
        }

        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Camera.Parameters parameters = mCamera.getParameters();

        int deviceOrientation = getResources().getConfiguration().orientation;

        if (display.getRotation() == Surface.ROTATION_0) {
            // Tablets can be rotated 0 degrees but be in landscape mode. Their cameras are
            // usually already aligned in the way we expect.
            if (deviceOrientation != Configuration.ORIENTATION_LANDSCAPE) {
                mCamera.setDisplayOrientation(90);
            }
        }
        else if(display.getRotation() == Surface.ROTATION_270) {
            if (deviceOrientation == Configuration.ORIENTATION_PORTRAIT) {
                mCamera.setDisplayOrientation(90);
            }
            else {
                mCamera.setDisplayOrientation(180);
            }
        }

        mCamera.setParameters(parameters);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        return true;
    }

    public Camera getCameraInstance(){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        int cameraId = settings.getInt("cameraId", 0);

        log("There are " + Camera.getNumberOfCameras() + " cameras.");
        log("Getting camera #" + cameraId);

        Camera c = null;

        try {
            c = Camera.open(cameraId);
        } catch (Exception e){
            // Camera is not available (in use or does not exist)
            // This case is handled in startCamera()
            log("Couldn't get camera instance", e);
        }

        if (null != c) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);

            ImageView imageView = (ImageView) findViewById(R.id.original_image);

            try {
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    fitImageInImageViewMirrored(originalPhotoUri, imageView);
                }
                else {
                    fitImageInImageView(originalPhotoUri, imageView);
                }
            } catch (FileNotFoundException e) {
                fatalAlert(R.string.error_original_photo_missing).show();
            }
        }

        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            ((FrameLayout) findViewById(R.id.camera_preview)).removeView(mPreview);
            mPreview = null;
        }
    }

    public void updatePreviewSize(int width, int height) {
        if (null == mCamera) {
            return;
        }

        int[] originalImageDimensions = getImageDimensions(originalPhotoUri);

        // If the image we're reenacting is portrait, ensure that the width/height pair reflect that.
        // Sometimes they get passed in opposite, depending on when the rotation happened (I think).
        if (
                (width > height && originalImageDimensions[0] < originalImageDimensions[1])
            ||
                (width < height && originalImageDimensions[0] > originalImageDimensions[1])
                ) {
            int tmp = width;
            width = height;
            height = tmp;
        }

        log("Passed to updatePreviewSize: " + width + "x" + height);

        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();

        for (Camera.Size size : supportedSizes) {
            log("Supported Size: " + size.width + "x" + size.height);
        }

        Camera.Size bestPreviewSize = getBestPreviewSize(supportedSizes, width, height);
        log("Setting camera preview to " + bestPreviewSize.width + "x" + bestPreviewSize.height);
        parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);

        int displayedPreviewWidth = bestPreviewSize.width;
        int displayedPreviewHeight = bestPreviewSize.height;

        // If the preview width/height doesn't match the original image orientation, swap them.
        // This won't affect the actual preview setting, only the calculations for the preview frame.
        if (originalImageDimensions[0] < originalImageDimensions[1]) {
            displayedPreviewWidth = bestPreviewSize.height;
            displayedPreviewHeight = bestPreviewSize.width;
        }

        int maxPreviewWidth = width;
        int maxPreviewHeight = height;

        log("Max preview size is " + maxPreviewWidth + "x" + maxPreviewHeight);

        int bestPreviewContainerHeight = maxPreviewHeight;
        int bestPreviewContainerWidth = (int) Math.round(((float) maxPreviewHeight / displayedPreviewHeight) * displayedPreviewWidth);

        if (bestPreviewContainerWidth > maxPreviewWidth) {
            // Using all of the available height overran the available width. Use the available width instead.
            bestPreviewContainerWidth = maxPreviewWidth;
            bestPreviewContainerHeight = (int) Math.round(((float) maxPreviewWidth / displayedPreviewWidth) * displayedPreviewHeight);
        }

        log("Setting preview container size to " + bestPreviewContainerWidth + "x" + bestPreviewContainerHeight);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(bestPreviewContainerWidth, bestPreviewContainerHeight);
        // Center the preview in the container.
        layoutParams.setMargins(
                Math.round((maxPreviewWidth - bestPreviewContainerWidth) / 2),
                Math.round((maxPreviewHeight - bestPreviewContainerHeight) / 2),
                Math.round((maxPreviewWidth - bestPreviewContainerWidth) / 2),
                Math.round((maxPreviewHeight - bestPreviewContainerHeight) / 2)
        );
        findViewById(R.id.camera_preview).setLayoutParams(layoutParams);

        try {
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            // Setting the preview size failed for some reason.
            alert(R.string.error_couldnt_set_preview_size).show();
        }
    }

    private Camera.Size getBestPreviewSize(final List<Camera.Size> sizes, int w, int h) {
        Camera.Size bestSize = sizes.get(0);
        double bestRatio = 0;

        // I'm pretty sure that the camera sizes are always given in landscape mode.
        if (h > w) {
            int tmp = h;
            h = w;
            w = tmp;
        }

        double ratioToMatch = (double) w / h;

        for (Camera.Size size : sizes) {
            double thisRatio = (double) size.width / size.height;

            if (bestRatio == 0 || Math.abs(thisRatio - ratioToMatch) < Math.abs(bestRatio - ratioToMatch)) {
                bestRatio = thisRatio;
                bestSize = size;
            }
        }

        return bestSize;
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, final Camera camera) {
            // The taken photo will be rotated based on the rotation of the device.
            // Rotate it to the orientation that we expect.
            int deviceOrientation = getResources().getConfiguration().orientation;

            log("Orientation: " + deviceOrientation);

            if ( deviceOrientation != Configuration.ORIENTATION_LANDSCAPE ) {
                Bitmap storedBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, null);
                Matrix mat = new Matrix();

                switch (deviceOrientation) {
                    case Configuration.ORIENTATION_PORTRAIT:
                        log("Rotating 90.");
                        mat.postRotate(90);
                        break;
                }

                storedBitmap = Bitmap.createBitmap(storedBitmap, 0, 0, storedBitmap.getWidth(), storedBitmap.getHeight(), mat, true);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                storedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                data = stream.toByteArray();

                storedBitmap = null;
                mat = null;
            }

            File tempOutputDir = getCacheDir();
            File newPhotoTempFile;

            try {
                newPhotoTempFile = File.createTempFile("reenact", "jpg", tempOutputDir);
            } catch (IOException e){
                log("Couldn't create temp file to save new photo.");

                fatalAlert(R.string.error_couldnt_save_single_file).show();

                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(newPhotoTempFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                log("File not found: " + e.getMessage());

                fatalAlert(R.string.error_couldnt_copy_single_file).show();

                return;
            } catch (IOException e) {
                log("Error accessing file: " + e.getMessage());

                fatalAlert(R.string.error_couldnt_copy_single_file).show();

                return;
            } finally {
                log("Finished writing file.");
            }

            // Start the confirmation activity.
            Intent intent = new Intent(CaptureActivity.this, ConfirmActivity.class);
            intent.putExtra(ORIGINAL_PHOTO_PATH, originalPhotoUri);
            intent.putExtra(NEW_PHOTO_TEMP_PATH, Uri.fromFile(newPhotoTempFile));
            startActivity(intent);

            data = null;
        }
    };

    private void fadeOriginalImageInAndOut(final ImageView img) {
        fadeOutImage(img);
    }

    private void fadeOutImage(final ImageView img) {
        Animation fadeOut = new AlphaAnimation(0.85f, 0);
        fadeOut.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeOut.setDuration(2500);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(final Animation animation) {
                fadeInImage(img);
            }

            public void onAnimationRepeat(final Animation animation) {
            }

            public void onAnimationStart(final Animation animation) {
            }
        });

        img.startAnimation(fadeOut);
    }

    private void fadeInImage(final ImageView img){
        final Animation fadeIn = new AlphaAnimation(0, 0.85f);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeIn.setDuration(2500);

        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                fadeOutImage(img);
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {}
        });

        img.startAnimation(fadeIn);
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("onStop");
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume");
        initializeOriginalPhoto();
        startCamera();
    }

    public void switchCamera(final View view) {
        releaseCamera();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        int cameraId = settings.getInt("cameraId", 0);
        cameraId++;

        cameraId %= Camera.getNumberOfCameras();

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("cameraId", cameraId);
        editor.commit();

        startCamera();
    }

    public void startPreviewFailed(Exception e){
        log("Error setting camera preview: " + e.getMessage());

        fatalAlert(R.string.error_no_camera_preview).show();
    }
}