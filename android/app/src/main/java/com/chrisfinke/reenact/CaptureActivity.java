package com.chrisfinke.reenact;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
        releaseCamera();              // release the camera immediately on pause event
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

    public static Camera getCameraInstance(){
        Camera c = null;

        try {
            c = Camera.open();
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
            mPreview = null;
        }
    }

    public void updatePreviewSize( int width, int height ) {
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Camera.Parameters parameters = mCamera.getParameters();

        if(display.getRotation() == Surface.ROTATION_0)
        {
            parameters.setPreviewSize(height, width);
        }

        if(display.getRotation() == Surface.ROTATION_90)
        {
            parameters.setPreviewSize(width, height);
        }

        if(display.getRotation() == Surface.ROTATION_180)
        {
            parameters.setPreviewSize(height, width);
        }

        if(display.getRotation() == Surface.ROTATION_270)
        {
            parameters.setPreviewSize(width, height);
        }

        mCamera.setParameters(parameters);
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
}
