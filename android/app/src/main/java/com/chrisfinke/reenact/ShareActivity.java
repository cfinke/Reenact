package com.chrisfinke.reenact;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ShareActivity extends ReenactActivity {
    Uri mergedPhotoUri;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        Toast.makeText(getApplicationContext(), getResources().getText(R.string.photo_saved_alert), Toast.LENGTH_SHORT).show();

        Intent intent = getIntent();
        mergedPhotoUri = intent.getParcelableExtra(MERGED_PHOTO_PATH);

        ImageView imageViewMerged = (ImageView) findViewById(R.id.image_merged);

        InputStream imageStream = null;

        Point windowSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(windowSize);

        Log.d(LOG_TAG, "imageView max size: " + windowSize.x + "x" + windowSize.y);

        // This final image is close to square, so it won't be wider than the smallest window dimension.
        int sampleSize = getOptimalSampleSize(mergedPhotoUri, Math.min(windowSize.x, windowSize.y), Math.min(windowSize.x, windowSize.y));

        // These images are side-by-side, so they take up at most a quarter of the size they would if they were full screen.
        sampleSize *= 2;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = sampleSize;

        try {
            imageStream = getContentResolver().openInputStream(mergedPhotoUri);
            imageViewMerged.setImageBitmap(BitmapFactory.decodeStream(imageStream, null, bitmapOptions));
        } catch (FileNotFoundException e) {
            AlertDialog alertDialog = buildFatalAlert();
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

    public void startOver(final View view) {
        onBackPressed();
    }
}
