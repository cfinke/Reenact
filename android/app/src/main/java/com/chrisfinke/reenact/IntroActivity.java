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
                Util.PICK_IMAGE_TO_REENACT
        );
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.d(Util.LOG_TAG, "Activity ended.");

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){
            Log.d(Util.LOG_TAG, "Result code ok.");

            if (requestCode == Util.PICK_IMAGE_TO_REENACT){
                Log.d(Util.LOG_TAG, "Requestcode ok.");

                Uri selectedImageUri = data.getData();

                Log.d(Util.LOG_TAG, selectedImageUri.toString());

                Intent intent = new Intent(this, CaptureActivity.class);
                intent.putExtra(Util.ORIGINAL_PHOTO_PATH, selectedImageUri);
                startActivity(intent);
            }
        }
        else {
            Log.d(Util.LOG_TAG, "activityResult was not OK.");
        }
    }

    public void showHelp(View view) {
        View helpView = findViewById(R.id.help);
        helpView.setVisibility(View.VISIBLE);
    }

    public void hideHelp(View view) {
        View helpView = findViewById(R.id.help);
        helpView.setVisibility(View.INVISIBLE);
    }
}
