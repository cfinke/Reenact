package com.chrisfinke.reenact;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.graphics.Matrix;

public class ConfirmActivity extends ReenactActivity {
    private Uri originalPhotoUri;
    private Uri newPhotoTempUri;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        Intent intent = getIntent();
        originalPhotoUri = intent.getParcelableExtra(ORIGINAL_PHOTO_PATH);
        newPhotoTempUri = intent.getParcelableExtra(NEW_PHOTO_TEMP_PATH);

        ImageView imageViewThen = (ImageView) findViewById(R.id.image_then);
        ImageView imageViewNow = (ImageView) findViewById(R.id.image_now);

        int[] oldImageDimensions = getImageDimensions(originalPhotoUri);
        int[] newImageDimensions = getImageDimensions(newPhotoTempUri);

        // Adjust the sizes of the thumbnails so that portrait images are the same height
        // as each other and landscape are the same width.
        int oldImageWidth = oldImageDimensions[0];
        int oldImageHeight = oldImageDimensions[1];

        int newImageWidth = newImageDimensions[0];
        int newImageHeight = newImageDimensions[1];

        log("oldImageHeight: " + oldImageHeight);
        log("newImageHeight: " + newImageHeight);

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

            log("sameHeightOldImageWidth: " + sameHeightOldImageWidth);
            log("sameHeightNewImageWidth: " + sameHeightNewImageWidth);

            log("oldImageWeight: " + oldImageWeight);
            log("newImageWeight: " + newImageWeight);

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

        flipViewForRTL(R.id.back_button);
    }

    public void goBack() {
        super.onBackPressed();
        finish();
    }

    public void goBack(final View view) {
        goBack();
    }

    @Override
    public void onSwipeRight() {
        super.onSwipeRight();
        goBack();
    }

    private static File getOutputMediaFile(final int type, final String prefix){
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ),
                "Reenact"
        );

        try {
            if ( ! Environment.getExternalStorageState(mediaStorageDir).equals(Environment.MEDIA_MOUNTED)) {
                log("External media storage is not mounted.");
                return null;
            }
        } catch (java.lang.NoSuchMethodError e) {
            log("Running in a pre-getExternalStorageState context.", e);
        }

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                log("failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File mediaFile;

        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + prefix + timeStamp + ".jpg");
        }
        else {
            return null;
        }

        return mediaFile;
    }

    public void confirm(final View view) {
        // Save the new image by itself.

        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, "IMG_");

        if (pictureFile == null) {
            fatalAlert(R.string.error_couldnt_save_single_file).show();
            return;
        }

        log("Single image: " + pictureFile.toString());

        try {
            copy(new File(newPhotoTempUri.getPath()), pictureFile);
        } catch (IOException e){
            log("Couldn't copy new photo", e);
            fatalAlert(R.string.error_couldnt_copy_single_file).show();
            return;
        }

        Bitmap combinedImage = combineImages(originalPhotoUri, newPhotoTempUri);

        if (combinedImage == null){
            // The work of exiting out of the activity has already been done inside of combineImages()
            return;
        }

        pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, "Reenacted_IMG_");

        log("Merged image: " + pictureFile.toString());

        if (pictureFile == null) {
            log("Error creating media file, check storage permissions: ");
            fatalAlert(R.string.error_couldnt_save_merged_file).show();
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            combinedImage.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            fos = null;
        } catch (FileNotFoundException e) {
            log("File not found: " + e.getMessage());
            alert(R.string.error_couldnt_copy_merged_file).show();
            return;
        } catch (IOException e) {
            log("Error accessing file: " + e.getMessage());
            alert(R.string.error_couldnt_copy_merged_file).show();
            return;
        } finally {
            log("Finished writing file.");
        }

        Uri mergedPhotoUri = Uri.fromFile(pictureFile);
        Intent intent = new Intent(this, ShareActivity.class);
        intent.putExtra(MERGED_PHOTO_PATH, mergedPhotoUri);
        startActivity(intent);

        pictureFile = null;
        finish();
    }

    /**
     * Given two images, combine them into a comparison shot. If they're landscape
     * images, put them top and bottom; if they're portrait, side-by-side.
     */

    public Bitmap combineImages(final Uri thenImage, final Uri nowImage) {
        int[] thenImageDimensions = getImageDimensions(thenImage);

        int cWidth = thenImageDimensions[0];
        int cHeight = thenImageDimensions[1];

        if (cWidth == 0 || cHeight == 0) {
            alert(R.string.error_couldnt_read_original_photo).show();
            return null;
        }

        int[] nowImageDimensions = getImageDimensions(nowImage);

        int sWidth = nowImageDimensions[0];
        int sHeight = nowImageDimensions[1];

        if (sWidth == 0 || sHeight == 0) {
            alert(R.string.error_couldnt_read_new_photo).show();
            return null;
        }

        int totalHeight;
        int totalWidth;
        float oldRatio;
        float newRatio;

        Rect oldImageDest;
        Rect newImageDest;

        if (cWidth < cHeight) {
            log("Saving combination image in side-by-side format.");

            int smallestHeight = Math.min(1024, Math.min(cHeight, sHeight));
            totalHeight = smallestHeight;

            int newOldHeight = totalHeight;
            int newOldWidth = Math.round(((float) smallestHeight / cHeight) * cWidth);

            int newNewHeight = totalHeight;
            int newNewWidth = Math.round(((float) smallestHeight / sHeight) * sWidth);

            totalWidth = newOldWidth + newNewWidth;

            oldRatio = (float) smallestHeight / cHeight;
            newRatio = (float) smallestHeight / sHeight;

            if (isRTL()) {
                // RTL languages would expect the "before" shot on the right.
                oldImageDest = new Rect(newNewWidth, 0, newOldWidth + newNewWidth, newOldHeight);
                newImageDest = new Rect(0, 0, newNewWidth, newNewHeight);
            }
            else {
                oldImageDest = new Rect(0, 0, newOldWidth, newOldHeight);
                newImageDest = new Rect(newOldWidth, 0, newOldWidth + newNewWidth, newNewHeight);
            }
        }
        else {
            log("Saving combination image in top-to-bottom format.");

            int smallestWidth = Math.min(1024, Math.min(cWidth, sWidth));
            totalWidth = smallestWidth;

            int newOldWidth = totalWidth;
            int newOldHeight = Math.round(((float) smallestWidth / cWidth) * cHeight);

            int newNewWidth = totalWidth;
            int newNewHeight = Math.round(((float) smallestWidth / sWidth) * sHeight);

            totalHeight = newOldHeight + newNewHeight;

            oldRatio = (float) smallestWidth / cWidth;
            newRatio = (float) smallestWidth / sWidth;

            oldImageDest = new Rect(0, 0, newOldWidth, newOldHeight);
            newImageDest = new Rect(0, newOldHeight, newNewWidth, newOldHeight + newNewHeight);
        }

        Bitmap cs = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);

        Canvas comboImage = new Canvas(cs);

        int oldSampleSize = (int) Math.max(1, Math.floor((float) 1 / oldRatio));
        int newSampleSize = (int) Math.max(1, Math.floor((float) 1 / newRatio));

        log("oldRatio:" + oldRatio);
        log("newRatio:" + newRatio);

        log("oldSampleSize:" + oldSampleSize);
        log("newSampleSize:" + newSampleSize);

        BitmapFactory.Options oldOptions = new BitmapFactory.Options();
        oldOptions.inSampleSize = oldSampleSize;

        AssetFileDescriptor oldFileDescriptor;

        try {
            oldFileDescriptor = getContentResolver().openAssetFileDescriptor(thenImage, "r");
        } catch (FileNotFoundException e) {
            log("File not found", e);

            alert(R.string.error_original_photo_missing).show();
            return null;
        }

        BitmapFactory.decodeFileDescriptor(oldFileDescriptor.getFileDescriptor(), null, oldOptions);
        Bitmap oldImage = BitmapFactory.decodeFileDescriptor(oldFileDescriptor.getFileDescriptor(), null, oldOptions);

        float orientation = (float) getOrientation(thenImage);

        if (orientation > 0) {
            Matrix rotationMatrix = new Matrix();
            rotationMatrix.postRotate(orientation);
            oldImage = Bitmap.createBitmap(oldImage, 0, 0, oldImage.getWidth(), oldImage.getHeight(), rotationMatrix, true);
        }

        comboImage.drawBitmap(oldImage, new Rect(0, 0, oldImage.getWidth(), oldImage.getHeight()), oldImageDest, null);

        oldImage.recycle();
        oldImage = null;
        oldOptions = null;
        oldFileDescriptor = null;

        BitmapFactory.Options newOptions = new BitmapFactory.Options();
        newOptions.inSampleSize = newSampleSize;

        AssetFileDescriptor newFileDescriptor;

        try {
            newFileDescriptor = getContentResolver().openAssetFileDescriptor(nowImage, "r");
        } catch (FileNotFoundException e) {
            log("File not found", e);

            alert(R.string.error_new_photo_missing).show();

            return null;
        }

        BitmapFactory.decodeFileDescriptor(newFileDescriptor.getFileDescriptor(), null, newOptions);
        Bitmap newImage = BitmapFactory.decodeFileDescriptor(newFileDescriptor.getFileDescriptor(), null, newOptions);

        comboImage.drawBitmap(newImage, new Rect(0, 0, newImage.getWidth(), newImage.getHeight()), newImageDest, null);

        newImage.recycle();
        newImage = null;
        newOptions = null;
        newFileDescriptor = null;

        // Add the Reenact logo to the lower right corner.
        Resources resources = getResources();
        Bitmap logoBitmap = BitmapFactory.decodeResource(resources, R.drawable.logo);

        int logoWidth = (int) Math.round(totalWidth * 0.04);
        int logoOffset = (int) Math.round(totalWidth * 0.01);

        Rect logoSrc = new Rect(0, 0, logoBitmap.getWidth(), logoBitmap.getHeight());
        Rect logoDest;

        if (isRTL()) {
            logoDest = new Rect(logoOffset, cs.getHeight() - logoWidth - logoOffset, logoOffset + logoWidth, cs.getHeight() - logoOffset);
        }
        else {
            logoDest = new Rect(cs.getWidth() - logoWidth - logoOffset, cs.getHeight() - logoWidth - logoOffset, cs.getWidth() - logoOffset, cs.getHeight() - logoOffset);
        }

        comboImage.drawBitmap(logoBitmap, logoSrc, logoDest, null);

        return cs;
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeOriginalPhoto();
        initializeNewPhoto();
    }

    @Override
    protected void onPause() {
        super.onPause();
        clearOriginalPhoto();
        clearNewPhoto();
    }

    private void initializeOriginalPhoto(){
        ImageView imageViewThen = (ImageView) findViewById(R.id.image_then);

        try {
            fitImageInImageView(originalPhotoUri, imageViewThen);
        } catch (FileNotFoundException e) {
            fatalAlert(R.string.error_new_photo_missing).show();
        }
    }

    private void initializeNewPhoto(){
        ImageView imageViewNow = (ImageView) findViewById(R.id.image_now);

        try {
            fitImageInImageView(newPhotoTempUri, imageViewNow);
        } catch (FileNotFoundException e) {
            fatalAlert(R.string.error_original_photo_missing).show();
        }
    }

    private void clearOriginalPhoto(){
        ImageView imageView = (ImageView) findViewById(R.id.image_then);
        imageView.setImageBitmap(null);
    }

    private void clearNewPhoto(){
        ImageView imageView = (ImageView) findViewById(R.id.image_now);
        imageView.setImageBitmap(null);
    }

    public void copy(final File src, final File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
}
