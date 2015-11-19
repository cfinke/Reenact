package com.chrisfinke.reenact;

import android.app.AlertDialog;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private Camera mCamera;
    CaptureActivity activityContext;

    private String LOG_TAG = "reenact";

    public CameraPreview(CaptureActivity context, Camera camera) {
        super(context);

        activityContext = context;
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        SurfaceHolder mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        if ( mCamera != null ) {
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d(LOG_TAG, "Error setting camera preview: " + e.getMessage());
                activityContext.startPreviewFailed();
            }
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        Log.d(LOG_TAG, "Surface Changed");

        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            Log.d(LOG_TAG, "stopPreview failed");
            // Don't do anything about this, since it's possible that it's irrelevant to the user.
        }

        // Update the preview here.
        activityContext.updatePreviewSize(width, height);

        try {
            mCamera.startPreview();
        } catch (Exception e) {
            activityContext.startPreviewFailed();
            return;
        }
    }
}
