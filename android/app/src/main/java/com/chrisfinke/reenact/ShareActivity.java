package com.chrisfinke.reenact;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ShareActivity extends Activity {
    Uri mergedPhotoUri;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        Toast.makeText(getApplicationContext(), getResources().getText(R.string.photo_saved_alert), Toast.LENGTH_SHORT).show();

        Intent intent = getIntent();
        mergedPhotoUri = intent.getParcelableExtra(Util.MERGED_PHOTO_PATH);

        ImageView imageViewMerged = (ImageView) findViewById(R.id.image_merged);

        InputStream imageStream = null;

        Point windowSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(windowSize);

        Log.d(Util.LOG_TAG, "imageView max size: " + windowSize.x + "x" + windowSize.y);

        // This final image is close to square, so it won't be wider than the smallest window dimension.
        int sampleSize = getOptimalSampleSize(mergedPhotoUri, Math.min(windowSize.x, windowSize.y), Math.min(windowSize.x, windowSize.y));

        // These images are side-by-side, so they take up at most a quarter of the size they would if they were full screen.
        sampleSize *= 2;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = sampleSize;

        try {
            imageStream = getContentResolver().openInputStream(mergedPhotoUri);
            imageViewMerged.setImageBitmap(BitmapFactory.decodeStream(imageStream, null, bitmapOptions));
        } catch (FileNotFoundException e) {
            AlertDialog alertDialog = Util.buildFatalAlert(ShareActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_merged_photo_missing));
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

    public void share(final View view) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/jpg");
        shareIntent.putExtra(Intent.EXTRA_STREAM, mergedPhotoUri);
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_dialog_label)));
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, IntroActivity.class));
        finish();
    }

    public void startOver(final View view) {
        onBackPressed();
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

    private int getOptimalSampleSize(final Uri imageUri, final int maxWidth, final int maxHeight) {
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
}
