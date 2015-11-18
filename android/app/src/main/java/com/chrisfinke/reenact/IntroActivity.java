package com.chrisfinke.reenact;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class IntroActivity extends Activity {
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
                Constants.PICK_IMAGE_TO_REENACT
        );
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.d(Constants.LOG_TAG, "Activity ended.");

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){
            Log.d(Constants.LOG_TAG, "Result code ok.");

            if (requestCode == Constants.PICK_IMAGE_TO_REENACT){
                Log.d(Constants.LOG_TAG, "Requestcode ok.");

                Uri selectedImageUri = data.getData();

                Log.d(Constants.LOG_TAG, selectedImageUri.toString());

                Intent intent = new Intent(this, CaptureActivity.class);
                intent.putExtra(Constants.ORIGINAL_PHOTO_PATH, selectedImageUri);
                startActivity(intent);
            }
        }
        else {
            Log.d(Constants.LOG_TAG, "activityResult was not OK.");
            // @todo
        }
    }

    public void showHelp(View view) {
        startActivity(new Intent(this, HelpActivity.class));
    }
}
