package com.chrisfinke.reenact;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class IntroActivity extends Activity {
    public final static Integer PICK_IMAGE_TO_REENACT = 1;
    public final static String ORIGINAL_PHOTO_PATH = "com.chrisfinke.reenact.ORIGINAL_PHOTO_PATH";

    private String LOG_TAG = "reenact";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
    }

    public void choosePhoto(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(
                Intent.createChooser(intent, getResources().getText(R.string.choose_photo_label)),
                PICK_IMAGE_TO_REENACT
        );
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.d(LOG_TAG, "Activity ended.");

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){
            Log.d(LOG_TAG, "Result code ok.");

            if (requestCode == PICK_IMAGE_TO_REENACT){
                Log.d(LOG_TAG, "Requestcode ok.");

                Uri selectedImageUri = data.getData();

                Log.d(LOG_TAG, selectedImageUri.toString());
            }
        }
        else {
            Log.d(LOG_TAG, "activityResult was not OK.");
            // @todo
        }
    }
}
