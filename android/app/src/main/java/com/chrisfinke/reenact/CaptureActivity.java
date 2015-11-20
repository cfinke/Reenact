package com.chrisfinke.reenact;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CaptureActivity extends Activity {
    private Uri originalPhotoUri;

    public String orientation = "portrait";

    private Camera mCamera;
    private CameraPreview mPreview;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        Intent intent = getIntent();
        originalPhotoUri = intent.getParcelableExtra(Util.ORIGINAL_PHOTO_PATH);

        Log.d(Util.LOG_TAG, "Received original photo URI: " + originalPhotoUri.toString());

        int[] originalImageDimensions = getImageDimensions(originalPhotoUri);

        int imageHeight = originalImageDimensions[1];
        int imageWidth = originalImageDimensions[0];

        Log.d(Util.LOG_TAG, "Original image dimensions: " + imageWidth + "x" + imageHeight);

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
            Log.d(Util.LOG_TAG, "Only one camera. Hiding switch button.");
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
    }

    public void goBack(final View view) {
        super.onBackPressed();

        Log.d(Util.LOG_TAG, "Going back.");

        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(Util.LOG_TAG, "onPause");

        releaseCamera();
        clearOriginalPhoto();
    }

    private void initializeOriginalPhoto(){
        ImageView imageView = (ImageView) findViewById(R.id.original_image);
        fadeOriginalImageInAndOut(imageView);

        InputStream imageStream = null;

        try {
            imageStream = getContentResolver().openInputStream(originalPhotoUri);
            imageView.setImageBitmap(BitmapFactory.decodeStream(imageStream));
        } catch (FileNotFoundException e) {
            AlertDialog alertDialog = Util.buildFatalAlert(CaptureActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_original_photo_missing));
            alertDialog.show();

            return;
        } finally {
            if (imageStream != null) {
                try {
                    imageStream.close();
                } catch (IOException e) {
                    // Ignorable?
                }
            }
        }
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
            AlertDialog alertDialog = Util.buildFatalAlert(CaptureActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_no_camera));
            alertDialog.show();

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
        SharedPreferences settings = getSharedPreferences(Util.PREFS_NAME, 0);
        int cameraId = settings.getInt("cameraId", 0);

        Log.d(Util.LOG_TAG, "Getting camera #" + cameraId);

        Camera c = null;

        try {
            c = Camera.open(cameraId);
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            // This case is handled in startCamera()
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

    public void updatePreviewSize(final int width, final int height) {
        if (null == mCamera) {
            return;
        }

        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();

        for (Camera.Size size : supportedSizes) {
            Log.d(Util.LOG_TAG, "Supported Size: " + size.width + "x" + size.height);
        }

        Camera.Size bestPreviewSize = getBestPreviewSize(supportedSizes, width, height);

        if(display.getRotation() == Surface.ROTATION_0)
        {
            Log.d(Util.LOG_TAG, "Setting preview to " + bestPreviewSize.height + "x" + bestPreviewSize.width);
            parameters.setPreviewSize(bestPreviewSize.height, bestPreviewSize.width);
        }

        if(display.getRotation() == Surface.ROTATION_90)
        {
            Log.d(Util.LOG_TAG, "Setting preview to " + bestPreviewSize.width + "x" + bestPreviewSize.height);
            parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
        }

        if(display.getRotation() == Surface.ROTATION_180)
        {
            Log.d(Util.LOG_TAG, "Setting preview to " + bestPreviewSize.height + "x" + bestPreviewSize.width);
            parameters.setPreviewSize(bestPreviewSize.height, bestPreviewSize.width);
        }

        if(display.getRotation() == Surface.ROTATION_270)
        {
            Log.d(Util.LOG_TAG, "Setting preview to " + bestPreviewSize.width + "x" + bestPreviewSize.height);
            parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
        }

        mCamera.setParameters(parameters);
    }

    private Camera.Size getBestPreviewSize(final List<Camera.Size> sizes, final int w, final int h) {
        Camera.Size bestSize = sizes.get(0);
        double bestRatio = 0;
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

            Log.d(Util.LOG_TAG, "Orientation: " + deviceOrientation);

            if ( deviceOrientation != Configuration.ORIENTATION_LANDSCAPE ) {
                Bitmap storedBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, null);
                Matrix mat = new Matrix();

                switch (deviceOrientation) {
                    case Configuration.ORIENTATION_PORTRAIT:
                        Log.d(Util.LOG_TAG, "Rotating 90.");
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
                Log.d(Util.LOG_TAG, "Couldn't create temp file to save new photo.");

                AlertDialog alertDialog = Util.buildFatalAlert(CaptureActivity.this);
                alertDialog.setMessage(getResources().getText(R.string.error_couldnt_save_single_file));
                alertDialog.show();

                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(newPhotoTempFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(Util.LOG_TAG, "File not found: " + e.getMessage());

                AlertDialog alertDialog = Util.buildFatalAlert(CaptureActivity.this);
                alertDialog.setMessage(getResources().getText(R.string.error_couldnt_copy_single_file));
                alertDialog.show();

                return;
            } catch (IOException e) {
                Log.d(Util.LOG_TAG, "Error accessing file: " + e.getMessage());

                AlertDialog alertDialog = Util.buildFatalAlert(CaptureActivity.this);
                alertDialog.setMessage(getResources().getText(R.string.error_couldnt_copy_single_file));
                alertDialog.show();

                return;
            } finally {
                Log.d(Util.LOG_TAG, "Finished writing file.");
            }

            // Start the confirmation activity.
            Intent intent = new Intent(CaptureActivity.this, ConfirmActivity.class);
            intent.putExtra(Util.ORIGINAL_PHOTO_PATH, originalPhotoUri);
            intent.putExtra(Util.NEW_PHOTO_TEMP_PATH, Uri.fromFile(newPhotoTempFile));
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

        fadeIn.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation) {
                fadeOutImage(img);
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });

        img.startAnimation(fadeIn);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(Util.LOG_TAG, "onStop");
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Util.LOG_TAG, "onResume");
        initializeOriginalPhoto();
        startCamera();
    }

    public void switchCamera(final View view) {
        releaseCamera();

        SharedPreferences settings = getSharedPreferences(Util.PREFS_NAME, 0);
        int cameraId = settings.getInt("cameraId", 0);
        cameraId++;

        cameraId %= Camera.getNumberOfCameras();

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("cameraId", cameraId);
        editor.commit();

        startCamera();
    }

    public void startPreviewFailed(){
        AlertDialog alertDialog = Util.buildFatalAlert(CaptureActivity.this);
        alertDialog.setMessage(getResources().getText(R.string.error_no_camera_preview));
        alertDialog.show();
    }

    private int[] getImageDimensions(final Uri imageUri) {
        InputStream imageStream;

        int[] dimensions = new int[2];
        dimensions[0] = 0;
        dimensions[1] = 0;

        try {
            imageStream = getContentResolver().openInputStream(imageUri);
        } catch (FileNotFoundException e ){
            Log.d(Util.LOG_TAG, "FileNotFound", e);
            return dimensions;
        }

        try {
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(imageStream, false);

            Log.d(Util.LOG_TAG, "Image dimensions: " + decoder.getWidth() + "x" + decoder.getHeight());

            dimensions[0] = decoder.getWidth();
            dimensions[1] = decoder.getHeight();
        } catch (IOException e){
            Log.d(Util.LOG_TAG, "IOException", e);
            return dimensions;
        } finally {
            try {
                imageStream.close();
            } catch (IOException e) {
                //
            }
        }

        return dimensions;
    }
}