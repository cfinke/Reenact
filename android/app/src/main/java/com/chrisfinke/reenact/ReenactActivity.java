package com.chrisfinke.reenact;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ReenactActivity extends Activity {
    public final static Integer PICK_IMAGE_TO_REENACT = 1;

    public final static String ORIGINAL_PHOTO_PATH = "com.chrisfinke.reenact.ORIGINAL_PHOTO_PATH";
    public final static String NEW_PHOTO_TEMP_PATH = "com.chrisfinke.reenact.NEW_PHOTO_TEMP_PATH";
    public final static String MERGED_PHOTO_PATH = "com.chrisfinke.reenact.MERGED_PHOTO_PATH";

    public final static String LOG_TAG = "reenact";

    public static final int MEDIA_TYPE_IMAGE = 1;

    public static final String PREFS_NAME = "ReenactPrefs";

    static final boolean LOG = BuildConfig.DEBUG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            if (LOG) Log.d(LOG_TAG, "FileNotFound", e);
            return dimensions;
        }

        try {
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(imageStream, false);

            if (LOG) Log.d(LOG_TAG, "Image dimensions: " + decoder.getWidth() + "x" + decoder.getHeight());

            dimensions[0] = decoder.getWidth();
            dimensions[1] = decoder.getHeight();
        } catch (IOException e){
            if (LOG) Log.d(LOG_TAG, "IOException", e);
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

    protected final void fitImageInImageView(Uri imageUri, ImageView imageView) throws FileNotFoundException {
        InputStream imageStream = null;

        Point windowSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(windowSize);

        if (LOG) Log.d(LOG_TAG, "imageView max size: " + windowSize.x + "x" + windowSize.y);

        int sampleSize = getOptimalSampleSize(imageUri, windowSize.x, windowSize.y);

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = sampleSize;

        if (LOG) Log.d(LOG_TAG, "Using sampleSize " + sampleSize);

        imageStream = getContentResolver().openInputStream(imageUri);
        imageView.setImageBitmap(BitmapFactory.decodeStream(imageStream, null, bitmapOptions));
    }

    public void flipViewForRTL(final int viewId) {
        try {
            int layoutDirection = getResources().getConfiguration().getLayoutDirection();
            if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                findViewById(viewId).setScaleX(-1);
            }
        }
        catch (NoSuchMethodError e) {
            // getLayoutDirection doesn't exist until API 17
        }
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
}
