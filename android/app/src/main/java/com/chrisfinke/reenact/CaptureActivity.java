package com.chrisfinke.reenact;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CaptureActivity extends Activity {
    private Uri originalPhotoUri;

    public String orientation = "portrait";

    private Camera mCamera;
    private CameraPreview mPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        Intent intent = getIntent();
        originalPhotoUri = intent.getParcelableExtra(Constants.ORIGINAL_PHOTO_PATH);

        Log.d(Constants.LOG_TAG, "Received original photo URI: " + originalPhotoUri.toString());

        ImageView imageView = (ImageView) findViewById(R.id.original_image);
        fadeOriginalImageInAndOut(imageView);

        InputStream imageStream = null;

        try {
            imageStream = getContentResolver().openInputStream(originalPhotoUri);
            imageView.setImageBitmap(BitmapFactory.decodeStream(imageStream));
        } catch (FileNotFoundException e) {
            // @todo Deal with this.
        } finally {
            if (imageStream != null) {
                try {
                    imageStream.close();
                } catch (IOException e) {
                    // Ignorable?
                }
            }
        }

        int imageHeight = imageView.getDrawable().getIntrinsicHeight();
        int imageWidth = imageView.getDrawable().getIntrinsicWidth();

        if ( imageWidth > imageHeight ) {
            orientation = "landscape";
        }

        if ( orientation.equals("portrait") ) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        ImageView switchButton = (ImageView) findViewById(R.id.switch_camera);

        if (Camera.getNumberOfCameras() == 1) {
            Log.d(Constants.LOG_TAG, "Only one camera. Hiding switch button.");
            switchButton.setVisibility(View.INVISIBLE);
        }

        startCamera();

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

    public void goBack(View view) {
        super.onBackPressed();

        Log.d(Constants.LOG_TAG, "Going back.");

        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(Constants.LOG_TAG, "onPause");

        releaseCamera();
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        startCamera();
    }

    protected void startCamera() {
        // Create an instance of Camera
        mCamera = getCameraInstance();

        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Camera.Parameters parameters = mCamera.getParameters();

        if(display.getRotation() == Surface.ROTATION_0)
        {
            mCamera.setDisplayOrientation(90);
        }
        if(display.getRotation() == Surface.ROTATION_270)
        {
            mCamera.setDisplayOrientation(180);
        }

        mCamera.setParameters(parameters);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    public Camera getCameraInstance(){
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        int cameraId = settings.getInt("cameraId", 0);

        Log.d(Constants.LOG_TAG, "Getting camera #" + cameraId);

        Camera c = null;

        try {
            c = Camera.open(cameraId);
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            // @todo Handle all null return values.
        }

        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
            mPreview = null;
        }
    }

    public void updatePreviewSize( int width, int height ) {
        if (null == mCamera) {
            return;
        }

        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();

        for (Camera.Size size : supportedSizes) {
            Log.d(Constants.LOG_TAG, "Supported Size: " + size.width + "x" + size.height);
        }

        Camera.Size bestPreviewSize = getBestPreviewSize(supportedSizes, width, height);

        if(display.getRotation() == Surface.ROTATION_0)
        {
            Log.d(Constants.LOG_TAG, "Setting preview to " + bestPreviewSize.height + "x" + bestPreviewSize.width);
            parameters.setPreviewSize(bestPreviewSize.height, bestPreviewSize.width);
        }

        if(display.getRotation() == Surface.ROTATION_90)
        {
            Log.d(Constants.LOG_TAG, "Setting preview to " + bestPreviewSize.width + "x" + bestPreviewSize.height);
            parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
        }

        if(display.getRotation() == Surface.ROTATION_180)
        {
            Log.d(Constants.LOG_TAG, "Setting preview to " + bestPreviewSize.height + "x" + bestPreviewSize.width);
            parameters.setPreviewSize(bestPreviewSize.height, bestPreviewSize.width);
        }

        if(display.getRotation() == Surface.ROTATION_270)
        {
            Log.d(Constants.LOG_TAG, "Setting preview to " + bestPreviewSize.width + "x" + bestPreviewSize.height);
            parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
        }

        mCamera.setParameters(parameters);
    }

    private Camera.Size getBestPreviewSize(List<Camera.Size> sizes, int w, int h) {
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
        public void onPictureTaken(byte[] data, Camera camera) {
            // The taken photo will be rotated based on the rotation of the device.
            // Rotate it to the orientation that we expect.
            int deviceOrientation = getResources().getConfiguration().orientation;

            Log.d(Constants.LOG_TAG, "Orientation: " + deviceOrientation);

            if ( deviceOrientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE ) {
                Bitmap storedBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, null);
                Matrix mat = new Matrix();

                // @todo Use this in combinationg with getRotation() for tablets
                switch (deviceOrientation) {
                    case android.content.res.Configuration.ORIENTATION_PORTRAIT:
                        Log.d(Constants.LOG_TAG, "Rotating 90.");
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

            // Start the confirmation activity.
            Intent intent = new Intent(CaptureActivity.this, ConfirmActivity.class);
            intent.putExtra(Constants.ORIGINAL_PHOTO_PATH, originalPhotoUri);
            intent.putExtra(Constants.NEW_PHOTO_BYTES, data);
            startActivity(intent);

            data = null;
        }
    };

    private void fadeOriginalImageInAndOut(final ImageView img) {
        fadeOutImage(img);
    }

    private void fadeOutImage(final ImageView img) {
        Animation fadeOut = new AlphaAnimation(.75f, 0);
        fadeOut.setInterpolator(new LinearInterpolator());
        fadeOut.setDuration(2500);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                fadeInImage(img);
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        });

        img.startAnimation(fadeOut);
    }

    private void fadeInImage(final ImageView img){
        final Animation fadeIn = new AlphaAnimation(0, .75f);
        fadeIn.setInterpolator(new LinearInterpolator());
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
        Log.d(Constants.LOG_TAG, "onStop");
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Constants.LOG_TAG, "onResume");
        startCamera();
    }

    public void switchCamera(View view) {
        releaseCamera();

        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        int cameraId = settings.getInt("cameraId", 0);
        cameraId++;

        cameraId %= Camera.getNumberOfCameras();

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("cameraId", cameraId);
        editor.commit();

        startCamera();
    }

}