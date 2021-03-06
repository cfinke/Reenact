package com.chrisfinke.reenact;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class IntroActivity extends ReenactActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
    }

    public void choosePhoto(final View view) {
        if (screenshotMode){
            Uri selectedImageUri = Uri.parse("android.resource://com.chrisfinke.reenact/drawable/" + screenshotModeOrientation + "_old");

            log("screenshotOld: " + selectedImageUri.toString());

            Intent intent = new Intent(this, CaptureActivity.class);
            intent.putExtra(ORIGINAL_PHOTO_PATH, selectedImageUri);
            startActivity(intent);
        }
        else {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(
                    Intent.createChooser(intent, getResources().getText(R.string.choose_photo_label)),
                    PICK_IMAGE_TO_REENACT
            );
        }
    }

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data){
        log("Activity ended.");

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){
            log("Result code ok.");

            if (requestCode == PICK_IMAGE_TO_REENACT){
                log("Requestcode ok.");

                Uri selectedImageUri = data.getData();

                log(selectedImageUri.toString());

                Intent intent = new Intent(this, CaptureActivity.class);
                intent.putExtra(ORIGINAL_PHOTO_PATH, selectedImageUri);
                startActivity(intent);
            }
        }
        else {
            log("activityResult was not OK.");
        }
    }

    public void showHelp(final View view) {
        View helpView = findViewById(R.id.help);
        helpView.setVisibility(View.VISIBLE);
    }

    public void hideHelp(final View view) {
        View helpView = findViewById(R.id.help);
        helpView.setVisibility(View.INVISIBLE);
    }

    public void openTwitter(View view){
        Intent intent = null;

        try {
            // get the Twitter app if possible
            getPackageManager().getPackageInfo("com.twitter.android", 0);
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?screen_name=ReenactApp"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } catch (Exception e) {
            // no Twitter app, revert to browser
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/ReenactApp"));
        }

        startActivity(intent);
    }
}
