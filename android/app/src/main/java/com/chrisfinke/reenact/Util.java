package com.chrisfinke.reenact;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.content.Intent;

/**
 * Created by cfinke on 11/13/15.
 */
public class Util {
    public final static Integer PICK_IMAGE_TO_REENACT = 1;

    public final static String NEW_PHOTO_BYTES = "com.chrisfinke.reenact.NEW_PHOTO_BYTES";
    public final static String ORIGINAL_PHOTO_PATH = "com.chrisfinke.reenact.ORIGINAL_PHOTO_PATH";
    public final static String NEW_PHOTO_TEMP_PATH = "com.chrisfinke.reenact.NEW_PHOTO_TEMP_PATH";
    public final static String MERGED_PHOTO_PATH = "com.chrisfinke.reenact.MERGED_PHOTO_PATH";

    public final static String LOG_TAG = "reenact";

    public static final int MEDIA_TYPE_IMAGE = 1;

    public static final String PREFS_NAME = "ReenactPrefs";

    public static final AlertDialog buildFatalAlert(final Activity caller){
        AlertDialog alertDialog = buildAlert(caller);
        alertDialog.setTitle(caller.getResources().getText(R.string.fatal_error_alert_title));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, caller.getResources().getText(R.string.fatal_error_alert_button_label),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        caller.startActivity(new Intent(caller, IntroActivity.class));
                        caller.finish();
                    }
                });
        return alertDialog;
    }

    public static final AlertDialog buildAlert(final Activity caller){
        AlertDialog alertDialog = new AlertDialog.Builder(caller).create();
        alertDialog.setTitle(caller.getResources().getText(R.string.error_alert_title));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, caller.getResources().getText(R.string.error_alert_button_label),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        return alertDialog;
    }
}
