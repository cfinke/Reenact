package com.chrisfinke.reenact;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class CaptureActivity extends Activity {
    private Uri originalPhotoUri;
    private String LOG_TAG = "reenact";

    public String orientation = "portrait";

    private Camera mCamera;
    private CameraPreview mPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        Intent intent = getIntent();
        originalPhotoUri = intent.getParcelableExtra(IntroActivity.ORIGINAL_PHOTO_PATH);

        Log.d(LOG_TAG, "Received original photo URI: " + originalPhotoUri.toString());

        ImageView imageView = (ImageView) findViewById(R.id.original_image);

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
}
