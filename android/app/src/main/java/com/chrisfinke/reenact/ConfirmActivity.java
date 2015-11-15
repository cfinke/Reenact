package com.chrisfinke.reenact;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
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

            int sameWidthOldImageHeight = Math.round(( (float) shorterWidth / oldImageWidth ) * oldImageHeight);
            int sameWidthNewImageHeight = Math.round(( (float) shorterWidth / newImageWidth ) * newImageHeight);

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

    private static File getOutputMediaFile(int type, String prefix){
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ),
                "Reenact"
        );

        if (! Environment.getExternalStorageState(mediaStorageDir).equals(Environment.MEDIA_MOUNTED)) {
            Log.d(Constants.LOG_TAG, "External media storage is not mounted.");
            // @todo Handle all null returns.
            return null;
        }

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(Constants.LOG_TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File mediaFile;

        if (type == Constants.MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + prefix + timeStamp + ".jpg");
        }
        else {
            return null;
        }

        return mediaFile;
    }

    public void confirm(View view) {
        // Save the new image by itself.

        File pictureFile = getOutputMediaFile(Constants.MEDIA_TYPE_IMAGE, "IMG_");

        Log.d(Constants.LOG_TAG, "Single image: " + pictureFile.toString());

        if (pictureFile == null) {
            Log.d(Constants.LOG_TAG, "Error creating media file, check storage permissions: ");
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(newPhotoBytes);
            fos.close();
            fos = null;
        } catch (FileNotFoundException e) {
            Log.d(Constants.LOG_TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(Constants.LOG_TAG, "Error accessing file: " + e.getMessage());
        } finally {
            Log.d(Constants.LOG_TAG, "Finished writing file.");
        }

        pictureFile = null;

        ImageView oldImageView = (ImageView) findViewById(R.id.image_then);
        ImageView newImageView = (ImageView) findViewById(R.id.image_now);

        // Save the merged image pair.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        AssetFileDescriptor fileDescriptor;

        try {
            fileDescriptor = getContentResolver().openAssetFileDescriptor(originalPhotoUri, "r");
        } catch (FileNotFoundException e) {
            Log.d(Constants.LOG_TAG, "File not found", e);
            return;
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = 4;

        BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
        Bitmap oldImage = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);

        Bitmap newImage;
        options = new BitmapFactory.Options();
        options.inMutable = true;
        newImage = BitmapFactory.decodeByteArray(newPhotoBytes, 0, newPhotoBytes.length, options);

        Bitmap combinedImage = combineImages(oldImage, newImage);

        /*
        ImageView imageViewThen = (ImageView) findViewById(R.id.image_then);
        ImageView imageViewNow = (ImageView) findViewById(R.id.image_now);

        imageViewThen.setImageBitmap(combinedImage);
        imageViewNow.setImageBitmap(combinedImage);
        */

        pictureFile = getOutputMediaFile(Constants.MEDIA_TYPE_IMAGE, "Reenacted_IMG_");

        Log.d(Constants.LOG_TAG, "Merged image: " + pictureFile.toString());

        if (pictureFile == null) {
            Log.d(Constants.LOG_TAG, "Error creating media file, check storage permissions: ");
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            combinedImage.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.write(newPhotoBytes);
            fos.close();
            fos = null;
        } catch (FileNotFoundException e) {
            Log.d(Constants.LOG_TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(Constants.LOG_TAG, "Error accessing file: " + e.getMessage());
        } finally {
            Log.d(Constants.LOG_TAG, "Finished writing file.");
        }

        Uri mergedPhotoUri = Uri.fromFile(pictureFile);
        Intent intent = new Intent(this, ShareActivity.class);
        intent.putExtra(Constants.MERGED_PHOTO_PATH, mergedPhotoUri);
        startActivity(intent);

        pictureFile = null;
        finish();
    }

    /**
     * Given two images, combine them into a comparison shot. If they're landscape
     * images, put them top and bottom; if they're portrait, side-by-side.
     */

    public Bitmap combineImages(Bitmap c, Bitmap s) {
        Bitmap cs = null;

        int cWidth = c.getWidth();
        int cHeight = c.getHeight();
        int sWidth = s.getWidth();
        int sHeight = s.getHeight();

        if ( cWidth < cHeight ) {
            // Portrait
            Log.d(Constants.LOG_TAG, "Saving combination image in side-by-side format.");

            int smallestHeight = Math.min(cHeight, sHeight);
            int totalHeight = smallestHeight;

            int newOldHeight = totalHeight;
            int newOldWidth = Math.round(((float) smallestHeight / cHeight) * cWidth);

            int newNewHeight = totalHeight;
            int newNewWidth = Math.round(((float) smallestHeight / sHeight) * sWidth);

            int totalWidth = newOldWidth + newNewWidth;

            cs = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);

            Canvas comboImage = new Canvas(cs);
            comboImage.drawBitmap(c, new Rect(0, 0, cWidth, cHeight), new Rect(0, 0, newOldWidth, newOldHeight), null);
            comboImage.drawBitmap(s, new Rect(0, 0, sWidth, sHeight), new Rect(newOldWidth, 0, newOldWidth + newNewWidth, newNewHeight), null);
        }
        else {
            // Landscape
            Log.d(Constants.LOG_TAG, "Saving combination image in top-to-bottom format.");

            int smallestWidth = Math.min(cWidth, sWidth);
            int totalWidth = smallestWidth;

            int newOldWidth = totalWidth;
            int newOldHeight = Math.round(((float) smallestWidth / cWidth) * cHeight);

            int newNewWidth = totalWidth;
            int newNewHeight = Math.round(((float) smallestWidth / sWidth) * sHeight);

            int totalHeight = newOldHeight + newNewHeight;

            cs = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);

            Canvas comboImage = new Canvas(cs);
            comboImage.drawBitmap(c, new Rect(0, 0, cWidth, cHeight), new Rect(0, 0, newOldWidth, newOldHeight), null);
            comboImage.drawBitmap(s, new Rect(0, 0, sWidth, sHeight), new Rect(0, newOldHeight, newNewWidth, newOldHeight + newNewHeight), null);
        }

        Log.d(Constants.LOG_TAG, "Finished combining images.");

        return cs;
    }
}
