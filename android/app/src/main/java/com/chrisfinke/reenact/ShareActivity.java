package com.chrisfinke.reenact;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ShareActivity extends Activity {
    Uri mergedPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        Toast.makeText(getApplicationContext(), getResources().getText(R.string.photo_saved_alert), Toast.LENGTH_SHORT).show();

        Intent intent = getIntent();
        mergedPhotoUri = intent.getParcelableExtra(Util.MERGED_PHOTO_PATH);

        ImageView imageViewMerged = (ImageView) findViewById(R.id.image_merged);

        InputStream imageStream = null;

        try {
            imageStream = getContentResolver().openInputStream(mergedPhotoUri);
            imageViewMerged.setImageBitmap(BitmapFactory.decodeStream(imageStream));
        } catch (FileNotFoundException e) {
            AlertDialog alertDialog = Util.buildFatalAlert(ShareActivity.this);
            alertDialog.setMessage(getResources().getText(R.string.error_merged_photo_missing));
            alertDialog.show();
            return;
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

    public void share(View view) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/jpg");
        shareIntent.putExtra(Intent.EXTRA_STREAM, mergedPhotoUri);
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_dialog_label)));
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, IntroActivity.class);
        startActivity(intent);
        finish();
    }

    public void startOver(View view) {
        onBackPressed();
    }
}
