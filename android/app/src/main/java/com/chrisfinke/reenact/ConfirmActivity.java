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

        // Adjust the sizes of the thumbnails so that portrait images are the same height
        // as each other and landscape are the same width.
        int oldImageHeight = imageViewThen.getDrawable().getIntrinsicHeight();
        int newImageHeight = imageViewNow.getDrawable().getIntrinsicHeight();

        int oldImageWidth = imageViewThen.getDrawable().getIntrinsicWidth();
        int newImageWidth = imageViewNow.getDrawable().getIntrinsicWidth();

        Log.d(Constants.LOG_TAG, "oldImageHeight: " + oldImageHeight);
        Log.d(Constants.LOG_TAG, "newImageHeight: " + newImageHeight);

        LinearLayout previewContainer = (LinearLayout) findViewById(R.id.preview_container);

        if ( newImageWidth < newImageHeight ) {
            // Portrait.
            // Ensure that the thumbnails are side-by-side.
            previewContainer.setOrientation(LinearLayout.HORIZONTAL);

            int shorterHeight = Math.min( oldImageHeight, newImageHeight );

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
            oldParams.width = 0;
            oldParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
            imageViewThen.setLayoutParams( oldParams );

            LinearLayout.LayoutParams newParams = (LinearLayout.LayoutParams) imageViewNow.getLayoutParams();
            newParams.weight = newImageWeight;
            newParams.width = 0;
            newParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
            imageViewNow.setLayoutParams( newParams );
        }
        else {
            // Ensure that the thumbnails are top-to-bottom.
            previewContainer.setOrientation(LinearLayout.VERTICAL);

            int shorterWidth = Math.min( oldImageWidth, newImageWidth );

            int sameWidthOldImageHeight = ( shorterWidth / oldImageWidth ) * oldImageHeight;
            int sameWidthNewImageHeight = ( shorterWidth / newImageWidth ) * newImageHeight;

            float oldImageWeight = ( (float) sameWidthOldImageHeight ) / ( sameWidthOldImageHeight + sameWidthNewImageHeight );
            float newImageWeight = ( (float) sameWidthNewImageHeight ) / ( sameWidthOldImageHeight + sameWidthNewImageHeight );

            LinearLayout.LayoutParams oldParams = (LinearLayout.LayoutParams) imageViewThen.getLayoutParams();
            oldParams.weight = oldImageWeight;
            oldParams.height = 0;
            oldParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
            imageViewThen.setLayoutParams( oldParams );

            LinearLayout.LayoutParams newParams = (LinearLayout.LayoutParams) imageViewNow.getLayoutParams();
            newParams.weight = newImageWeight;
            newParams.height = 0;
            newParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
            imageViewNow.setLayoutParams(newParams);
        }
    }

    public void goBack(View view) {
        super.onBackPressed();
        finish();
    }
}
