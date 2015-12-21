package com.chrisfinke.reenact;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConfirmActivity extends ReenactActivity {
    private Uri originalPhotoUri;
    private Uri newPhotoTempUri;

    private int templateIndex = 0;

    private Object[][][] templates = {
            new Object[][]{new Object[]{Color.WHITE}, new Object[]{0, 0, 0, 0, 0}, new Object[]{""}} // No margins. Just side-by-side.
            , new Object[][]{new Object[]{Color.WHITE}, new Object[]{2, 2, 2, 2, 2}, new Object[]{""}} // A white frame around both photos.
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        Intent intent = getIntent();
        originalPhotoUri = intent.getParcelableExtra(ORIGINAL_PHOTO_PATH);
        newPhotoTempUri = intent.getParcelableExtra(NEW_PHOTO_TEMP_PATH);

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
        showLastTemplate();
    }

    @Override
    public void onSwipeLeft() {
        super.onSwipeRight();
        showNextTemplate();
    }

    public void showNextTemplate(final View view){
        showNextTemplate();
    }

    private void showNextTemplate() {
        clearComboPhoto();
        templateIndex += 1;
        templateIndex %= templates.length;
        initializeComboPhoto();
    }

    public void showLastTemplate(final View view){
        showLastTemplate();
    }

    private void showLastTemplate() {
        clearComboPhoto();
        templateIndex -= 1;

        if (templateIndex < 0){
            templateIndex = templates.length - 1;
        }
        initializeComboPhoto();
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
        PhotoTemplate template = new PhotoTemplate(this, thenImage, nowImage);

        Object[][] templateSettings = templates[templateIndex];

        template.setMargins((int) templateSettings[1][0], (int) templateSettings[1][1], (int) templateSettings[1][2], (int) templateSettings[1][3], (int) templateSettings[1][4]);
        template.setBackgroundColor((int) templateSettings[0][0]);

        return template.draw();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeComboPhoto();
    }

    @Override
    protected void onPause() {
        super.onPause();
        clearComboPhoto();
    }

    private void initializeComboPhoto(){
        ImageView imageView = (ImageView) findViewById(R.id.image_combined_preview);
        imageView.setImageBitmap(combineImages(originalPhotoUri, newPhotoTempUri));
    }

    private void clearComboPhoto(){
        ImageView imageView = (ImageView) findViewById(R.id.image_combined_preview);
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
