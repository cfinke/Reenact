package com.chrisfinke.reenact;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class CaptureActivity extends Activity {
    private Uri originalPhotoUri;
    private String LOG_TAG = "reenact";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        Intent intent = getIntent();
        originalPhotoUri = intent.getParcelableExtra(IntroActivity.ORIGINAL_PHOTO_PATH);

        Log.d(LOG_TAG, "Received original photo URI: " + originalPhotoUri.toString());

        ImageView imageView = (ImageView) findViewById(R.id.original_image);

        InputStream imageStream = null;

        try {
            imageStream = getContentResolver().openInputStream(originalPhotoUri);
            imageView.setImageBitmap(BitmapFactory.decodeStream(imageStream));
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

    }
}
