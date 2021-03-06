package com.chrisfinke.reenact;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Matrix;

public class ReenactActivity extends Activity implements GestureDetector.OnGestureListener {
    private GestureDetectorCompat mDetector;

    public final static Integer PICK_IMAGE_TO_REENACT = 1;

    public final static String ORIGINAL_PHOTO_PATH = "com.chrisfinke.reenact.ORIGINAL_PHOTO_PATH";
    public final static String NEW_PHOTO_TEMP_PATH = "com.chrisfinke.reenact.NEW_PHOTO_TEMP_PATH";
    public final static String MERGED_PHOTO_PATH = "com.chrisfinke.reenact.MERGED_PHOTO_PATH";

    public final static String LOG_TAG = "reenact";

    public static final int MEDIA_TYPE_IMAGE = 1;

    public static final String PREFS_NAME = "ReenactPrefs";

    static final boolean LOG = BuildConfig.DEBUG;

    public final boolean screenshotMode = false;
    public final String screenshotModeOrientation = "portrait";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDetector = new GestureDetectorCompat(this,this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent event) {
        log("onDown: " + event.toString());
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        log("onFling: " + event1.toString() + event2.toString());

        if (Math.abs(event1.getX() - event2.getX()) >= 200) {
            if (event1.getX() < event2.getX()) {
                if (isRTL()) {
                    onSwipeLeft();
                }
                else {
                    onSwipeRight();
                }
            }
            else {
                if (isRTL()) {
                    onSwipeRight();
                }
                else {
                    onSwipeLeft();
                }
            }
        }

        return true;
    }

    public void onSwipeRight() {}
    public void onSwipeLeft() {}

    @Override
    public void onLongPress(MotionEvent event) {
        log("onLongPress: " + event.toString());
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        log("onScroll: " + e1.toString() + e2.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        log("onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        log("onSingleTapUp: " + event.toString());
        return true;
    }

    protected int getOptimalSampleSize(final Uri imageUri, final int maxWidth, final int maxHeight) {
        int[] thenImageDimensions = getImageDimensions(imageUri);

        int cWidth = thenImageDimensions[0];
        int cHeight = thenImageDimensions[1];

        if (cWidth == 0 || cHeight == 0) {
            return 1;
        }

        float oldRatio;

        if (cWidth < cHeight) {
            int smallestHeight = Math.min(cHeight, maxHeight);
            oldRatio = (float) smallestHeight / cHeight;
        }
        else {
            int smallestWidth = Math.min(cWidth, maxWidth);
            oldRatio = (float) smallestWidth / cWidth;
        }

        int sampleSize = (int) Math.max(1, Math.floor((float) 1 / oldRatio));

        // Most images we're loading into views don't need to be super-high quality.
        // Pre-emptively sacrifice some quality for lower memory usage.
        sampleSize *= 2;

        return sampleSize;
    }

    protected int[] getImageDimensions(final Uri imageUri) {
        InputStream imageStream;

        int[] dimensions = new int[2];
        dimensions[0] = 0;
        dimensions[1] = 0;

        try {
            imageStream = getContentResolver().openInputStream(imageUri);
        } catch (FileNotFoundException e ){
            log("FileNotFound", e);
            return dimensions;
        }

        try {
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(imageStream, false);

            log("Image dimensions: " + decoder.getWidth() + "x" + decoder.getHeight());

            dimensions[0] = decoder.getWidth();
            dimensions[1] = decoder.getHeight();
        } catch (IOException e){
            log("IOException", e);
            return dimensions;
        } finally {
            try {
                imageStream.close();
            } catch (IOException e) {
                //
            }
        }

        int orientation = getOrientation(imageUri);
        log("Orientation is " + orientation);

        if (orientation == 90 || orientation == 270){
            int tmp = dimensions[0];
            dimensions[0] = dimensions[1];
            dimensions[1] = tmp;
        }

        return dimensions;
    }

    protected final AlertDialog fatalAlert(final int messageId){
        final Activity self = this;

        AlertDialog alertDialog = alert(messageId);
        alertDialog.setTitle(getResources().getText(R.string.fatal_error_alert_title));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getText(R.string.fatal_error_alert_button_label),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        startActivity(new Intent(self, IntroActivity.class));
                        finish();
                    }
                });
        return alertDialog;
    }

    protected final AlertDialog alert(final int messageId){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(getResources().getText(R.string.error_alert_title));
        alertDialog.setMessage(getResources().getText(messageId));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getText(R.string.error_alert_button_label),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        return alertDialog;
    }

    protected final void fitImageInImageViewMirrored(Uri imageUri, ImageView imageView) throws FileNotFoundException {
        Matrix matrix = new Matrix();

        float orientation = (float) getOrientation(imageUri);

        // The image data itself is rotated 90 degrees from its display, but the matrix operates on the pre-display data.
        if (orientation == 90 || orientation == 270) {
            matrix.preScale(1, -1);
        }
        else {
            matrix.preScale(-1, 1);
        }

        fitImageInImageViewWithMatrix(imageUri, imageView, matrix);
    }

    protected final void fitImageInImageView(Uri imageUri, ImageView imageView) throws FileNotFoundException {
        fitImageInImageViewWithMatrix(imageUri, imageView, new Matrix());
    }

    protected final void fitImageInImageViewWithMatrix(Uri imageUri, ImageView imageView, Matrix matrix) throws FileNotFoundException {
        InputStream imageStream = null;

        Point windowSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(windowSize);

        log("imageView max size: " + windowSize.x + "x" + windowSize.y);

        int sampleSize = getOptimalSampleSize(imageUri, windowSize.x, windowSize.y);

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = sampleSize;

        log("Using sampleSize " + sampleSize);

        imageStream = getContentResolver().openInputStream(imageUri);

        float orientation = (float) getOrientation(imageUri);

        if (orientation > 0) {
            matrix.postRotate(orientation);
        }

        Bitmap imageforView = BitmapFactory.decodeStream(imageStream, null, bitmapOptions);

        imageforView = Bitmap.createBitmap(imageforView, 0, 0, imageforView.getWidth(), imageforView.getHeight(), matrix, true);
        imageView.setImageBitmap(imageforView);
    }

    public void flipViewForRTL(final int viewId) {
        try {
            int layoutDirection = getResources().getConfiguration().getLayoutDirection();
            if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                flipView(viewId);
            }
        }
        catch (NoSuchMethodError e) {
            // getLayoutDirection doesn't exist until API 17
        }
    }

    public void flipView(final int viewId) {
        log("Flipping " + viewId);
        findViewById(viewId).setScaleX(-1);
    }

    public boolean isRTL(){
        try {
            int layoutDirection = getResources().getConfiguration().getLayoutDirection();
            if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                return true;
            }
        }
        catch (NoSuchMethodError e) {
            // getLayoutDirection doesn't exist until API 17
        }

        return false;
    }

    public int getOrientation(Uri photoUri) {
        Cursor cursor = getContentResolver().query(
                photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION },
                null, null, null
        );

        try {
            if (cursor.getCount() != 1) {
                cursor.close();
                return -1;
            }
        } catch (NullPointerException e) {
            log("Couldn't get cursor count.", e);
            return -1;
        }

        cursor.moveToFirst();
        int orientation = cursor.getInt(0);
        cursor.close();
        return orientation;
    }

    static public void log(String logMessage) {
        if (LOG) Log.d(LOG_TAG, logMessage);
    }

    static public void log(String logMessage, Throwable e) {
        if (LOG) Log.d(LOG_TAG, logMessage, e);
    }
}
