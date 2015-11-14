package com.chrisfinke.reenact;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
    }

    public void goBack(View view) {
        super.onBackPressed();
        finish();
    }
}
