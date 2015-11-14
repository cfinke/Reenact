package com.chrisfinke.reenact;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

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
    }
}
