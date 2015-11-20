package com.chrisfinke.reenact;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Point;
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

public class ConfirmActivity extends Activity {
    private Uri originalPhotoUri;
    private Uri newPhotoTempUri;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        Intent intent = getIntent();
        originalPhotoUri = intent.getParcelableExtra(Util.ORIGINAL_PHOTO_PATH);
        newPhotoTempUri = intent.getParcelableExtra(Util.NEW_PHOTO_TEMP_PATH);

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

        Log.d(Util.LOG_TAG, "oldImageHeight: " + oldImageHeight);
        Log.d(Util.LOG_TAG, "newImageHeight: " + newImageHeight);

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

            Log.d(Util.LOG_TAG, "sameHeightOldImageWidth: " + sameHeightOldImageWidth);
            Log.d(Util.LOG_TAG, "sameHeightNewImageWidth: " + sameHeightNewImageWidth);

            Log.d(Util.LOG_TAG, "oldImageWeight: " + oldImageWeight);
            Log.d(Util.LOG_TAG, "newImageWeight: " + newImageWeight);

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

    public void goBack(final View view) {
        super.onBackPressed();
        finish();
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
                Log.d(Util.LOG_TAG, "External media storage is not mounted.");
                return null;
            }
        } catch (java.lang.NoSuchMethodError e) {
            Log.d(Util.LOG_TAG, "Running in a pre-getExternalStorageState context.", e);
        }

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(Util.LOG_TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File mediaFile;

        if (type == Util.MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + prefix + timeStamp + ".jpg");
        }
        else {
            return null;
        }

        return mediaFile;
    }

    public void confirm(final View view) {
        // Save the new image by itself.

        File pictureFile = getOutputMediaFile(Util.MEDIA_TYPE_IMAGE, "IMG_");

        if (pictureFile == null) {
            AlertDialog alertDialog = Util.buildFatalAlert(ConfirmActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_couldnt_save_single_file));
            alertDialog.show();
            return;
        }

        Log.d(Util.LOG_TAG, "Single image: " + pictureFile.toString());

        try {
            copy(new File(newPhotoTempUri.getPath()), pictureFile);
        } catch (IOException e){
            Log.d(Util.LOG_TAG, "Couldn't copy new photo", e);
            AlertDialog alertDialog = Util.buildFatalAlert(ConfirmActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_couldnt_copy_single_file));
            alertDialog.show();
            return;
        }

        Bitmap combinedImage = combineImages(originalPhotoUri, newPhotoTempUri);

        if (combinedImage == null){
            // The work of exiting out of the activity has already been done inside of combineImages()
            return;
        }

        pictureFile = getOutputMediaFile(Util.MEDIA_TYPE_IMAGE, "Reenacted_IMG_");

        Log.d(Util.LOG_TAG, "Merged image: " + pictureFile.toString());

        if (pictureFile == null) {
            Log.d(Util.LOG_TAG, "Error creating media file, check storage permissions: ");
            AlertDialog alertDialog = Util.buildFatalAlert(ConfirmActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_couldnt_save_merged_file));
            alertDialog.show();
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            combinedImage.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            fos = null;
        } catch (FileNotFoundException e) {
            Log.d(Util.LOG_TAG, "File not found: " + e.getMessage());
            AlertDialog alertDialog = Util.buildAlert(ConfirmActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_couldnt_copy_merged_file));
            alertDialog.show();
            return;
        } catch (IOException e) {
            Log.d(Util.LOG_TAG, "Error accessing file: " + e.getMessage());
            AlertDialog alertDialog = Util.buildAlert(ConfirmActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_couldnt_copy_merged_file));
            alertDialog.show();
            return;
        } finally {
            Log.d(Util.LOG_TAG, "Finished writing file.");
        }

        Uri mergedPhotoUri = Uri.fromFile(pictureFile);
        Intent intent = new Intent(this, ShareActivity.class);
        intent.putExtra(Util.MERGED_PHOTO_PATH, mergedPhotoUri);
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
            AlertDialog alertDialog = Util.buildAlert(ConfirmActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_couldnt_read_original_photo));
            alertDialog.show();
            return null;
        }

        int[] nowImageDimensions = getImageDimensions(nowImage);

        int sWidth = nowImageDimensions[0];
        int sHeight = nowImageDimensions[1];

        if (sWidth == 0 || sHeight == 0) {
            AlertDialog alertDialog = Util.buildAlert(ConfirmActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_couldnt_read_new_photo));
            alertDialog.show();
            return null;
        }

        int totalHeight;
        int totalWidth;
        float oldRatio;
        float newRatio;

        Rect oldImageDest;
        Rect newImageDest;

        if (cWidth < cHeight) {
            Log.d(Util.LOG_TAG, "Saving combination image in side-by-side format.");

            int smallestHeight = Math.min(1024, Math.min(cHeight, sHeight));
            totalHeight = smallestHeight;

            int newOldHeight = totalHeight;
            int newOldWidth = Math.round(((float) smallestHeight / cHeight) * cWidth);

            int newNewHeight = totalHeight;
            int newNewWidth = Math.round(((float) smallestHeight / sHeight) * sWidth);

            totalWidth = newOldWidth + newNewWidth;

            oldRatio = (float) smallestHeight / cHeight;
            newRatio = (float) smallestHeight / sHeight;

            oldImageDest = new Rect(0, 0, newOldWidth, newOldHeight);
            newImageDest = new Rect(newOldWidth, 0, newOldWidth + newNewWidth, newNewHeight);
        }
        else {
            Log.d(Util.LOG_TAG, "Saving combination image in top-to-bottom format.");

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

        Log.d(Util.LOG_TAG, "oldRatio:" + oldRatio);
        Log.d(Util.LOG_TAG, "newRatio:" + newRatio);

        Log.d(Util.LOG_TAG, "oldSampleSize:" + oldSampleSize);
        Log.d(Util.LOG_TAG, "newSampleSize:" + newSampleSize);

        BitmapFactory.Options oldOptions = new BitmapFactory.Options();
        oldOptions.inSampleSize = oldSampleSize;

        AssetFileDescriptor oldFileDescriptor;

        try {
            oldFileDescriptor = getContentResolver().openAssetFileDescriptor(thenImage, "r");
        } catch (FileNotFoundException e) {
            Log.d(Util.LOG_TAG, "File not found", e);

            AlertDialog alertDialog = Util.buildAlert(ConfirmActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_original_photo_missing));
            alertDialog.show();

            return null;
        }

        BitmapFactory.decodeFileDescriptor(oldFileDescriptor.getFileDescriptor(), null, oldOptions);
        Bitmap oldImage = BitmapFactory.decodeFileDescriptor(oldFileDescriptor.getFileDescriptor(), null, oldOptions);

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
            Log.d(Util.LOG_TAG, "File not found", e);

            AlertDialog alertDialog = Util.buildAlert(ConfirmActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_new_photo_missing));
            alertDialog.show();

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
        Rect logoDest = new Rect(cs.getWidth() - logoWidth - logoOffset, cs.getHeight() - logoWidth - logoOffset, cs.getWidth() - logoOffset, cs.getHeight() - logoOffset);

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

        InputStream thenImageStream = null;

        Point windowSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(windowSize);

        Log.d(Util.LOG_TAG, "imageView max size: " + windowSize.x + "x" + windowSize.y);

        int sampleSize = getOptimalSampleSize(originalPhotoUri, windowSize.x, windowSize.y);

        // These images are side-by-side, so they take up at most a quarter of the size they would if they were full screen.
        sampleSize *= 2;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = sampleSize;

        Log.d(Util.LOG_TAG, "Using sampleSize " + sampleSize);

        try {
            thenImageStream = getContentResolver().openInputStream(originalPhotoUri);
            imageViewThen.setImageBitmap(BitmapFactory.decodeStream(thenImageStream, null, bitmapOptions));
        } catch (FileNotFoundException e) {
            AlertDialog alertDialog = Util.buildFatalAlert(ConfirmActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_original_photo_missing));
            alertDialog.show();
            return;
        } finally {
            if (thenImageStream != null) {
                try {
                    thenImageStream.close();
                } catch (IOException e) {
                    // Ignorable?
                }
            }
        }
    }

    private void initializeNewPhoto(){
        ImageView imageViewNow = (ImageView) findViewById(R.id.image_now);

        InputStream nowImageStream = null;

        Point windowSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(windowSize);

        Log.d(Util.LOG_TAG, "imageView max size: " + windowSize.x + "x" + windowSize.y);

        int sampleSize = getOptimalSampleSize(newPhotoTempUri, windowSize.x, windowSize.y);

        // These images are side-by-side, so they take up at most a quarter of the size they would if they were full screen.
        sampleSize *= 2;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = sampleSize;

        try {
            nowImageStream = getContentResolver().openInputStream(newPhotoTempUri);
            imageViewNow.setImageBitmap(BitmapFactory.decodeStream(nowImageStream, null, bitmapOptions));
        } catch (FileNotFoundException e) {
            AlertDialog alertDialog = Util.buildFatalAlert(ConfirmActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_new_photo_missing));
            alertDialog.show();
            return;
        } finally {
            if (nowImageStream != null) {
                try {
                    nowImageStream.close();
                } catch (IOException e) {
                    // Ignorable?
                }
            }
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
