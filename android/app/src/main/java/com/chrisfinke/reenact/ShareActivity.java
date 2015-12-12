package com.chrisfinke.reenact;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;

public class ShareActivity extends ReenactActivity {
    Uri mergedPhotoUri;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        Toast.makeText(getApplicationContext(), getResources().getText(R.string.photo_saved_alert), Toast.LENGTH_SHORT).show();

        mergedPhotoUri = getIntent().getParcelableExtra(MERGED_PHOTO_PATH);

        ImageView imageViewMerged = (ImageView) findViewById(R.id.image_merged);

        try {
            fitImageInImageView(mergedPhotoUri, imageViewMerged);
        } catch (FileNotFoundException e) {
            fatalAlert(R.string.error_merged_photo_missing).show();
        }

        // Flip the start-over button in RTL layouts.
        flipViewForRTL(R.id.button_start_over);
    }

    public void share(final View view) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/jpg");
        shareIntent.putExtra(Intent.EXTRA_STREAM, mergedPhotoUri);
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_dialog_label)));
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, IntroActivity.class));
        finish();
    }

    public void startOver() {
        onBackPressed();
    }

    public void startOver(final View view) {
        startOver();
    }

    @Override
    public void onSwipeRight() {
        super.onSwipeRight();
        startOver();
    }
}
