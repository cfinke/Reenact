package com.chrisfinke.reenact;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConfirmActivity extends Activity {
    private Uri originalPhotoUri;
    private byte[] newPhotoBytes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Intent intent = getIntent();
        originalPhotoUri = intent.getParcelableExtra(Constants.ORIGINAL_PHOTO_PATH);
        newPhotoBytes = intent.getByteArrayExtra(Constants.NEW_PHOTO_BYTES);

        ImageView imageViewThen = (ImageView) findViewById(R.id.image_then);
        ImageView imageViewNow = (ImageView) findViewById(R.id.image_now);

        InputStream imageStream = null;

        try {
            imageStream = getContentResolver().openInputStream(originalPhotoUri);
            imageViewThen.setImageBitmap(BitmapFactory.decodeStream(imageStream));
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

        Bitmap bMap = BitmapFactory.decodeByteArray(newPhotoBytes, 0, newPhotoBytes.length);
        imageViewNow.setImageBitmap(bMap);

        int oldImageHeight = imageViewThen.getDrawable().getIntrinsicHeight();
        int newImageHeight = imageViewNow.getDrawable().getIntrinsicHeight();

        int oldImageWidth = imageViewThen.getDrawable().getIntrinsicWidth();
        int newImageWidth = imageViewNow.getDrawable().getIntrinsicWidth();

        int adjustedOldImageHeight = Math.round((float) oldImageWidth / newImageWidth * oldImageHeight);
        int adjustedNewImageHeight = Math.round((float) oldImageWidth / newImageWidth * newImageHeight);

        Log.d(Constants.LOG_TAG, "oldImageHeight: " + oldImageHeight);
        Log.d(Constants.LOG_TAG, "newImageHeight: " + newImageHeight);

        if ( newImageWidth < newImageHeight ) {
            // Portrait.
            int shorterHeight = Math.min( oldImageHeight, newImageHeight );

            int sameHeightOldImageHeight = ( shorterHeight / oldImageHeight ) * oldImageHeight;
            int sameHeightNewImageHeight = ( shorterHeight / newImageHeight ) * newImageHeight;

            int sameHeightOldImageWidth = Math.round( ( (float) shorterHeight / oldImageHeight ) * oldImageWidth);
            int sameHeightNewImageWidth = Math.round( ( (float) shorterHeight / newImageHeight ) * newImageWidth);

            float oldImageWeight = ( (float) sameHeightOldImageWidth ) / ( sameHeightOldImageWidth + sameHeightNewImageWidth );
            float newImageWeight = ( (float) sameHeightNewImageWidth ) / ( sameHeightOldImageWidth + sameHeightNewImageWidth );

            Log.d(Constants.LOG_TAG, "sameHeightOldImageWidth: " + sameHeightOldImageWidth);
            Log.d(Constants.LOG_TAG, "sameHeightNewImageWidth: " + sameHeightNewImageWidth);

            Log.d(Constants.LOG_TAG, "oldImageWeight: " + oldImageWeight);
            Log.d(Constants.LOG_TAG, "newImageWeight: " + newImageWeight);

            LinearLayout.LayoutParams oldParams = (LinearLayout.LayoutParams) imageViewThen.getLayoutParams();
            oldParams.weight = oldImageWeight;
            imageViewThen.setLayoutParams( oldParams );

            LinearLayout.LayoutParams newParams = (LinearLayout.LayoutParams) imageViewNow.getLayoutParams();
            newParams.weight = newImageWeight;
            imageViewNow.setLayoutParams( newParams );
        }
        else {
        }
    }

    public void goBack(View view) {
        super.onBackPressed();
        finish();
    }
}
