package com.chrisfinke.reenact;

import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;

import java.io.FileNotFoundException;

/**
 * Created by cfinke on 12/11/15.
 */
public class PhotoTemplate {
    private ReenactActivity context;

    private int marginPercentLeft;
    private int marginPercentRight;
    private int marginPercentTop;
    private int marginPercentBottom;
    private int marginPercentCenter;

    private int backgroundColor = Color.WHITE;
    private String header = "";
    private String footer = "";

    private String orientation = "portrait";

    private Uri thenImage;
    private Uri nowImage;

    public PhotoTemplate(ReenactActivity suppliedContext) {
        context = suppliedContext;
    }

    public PhotoTemplate(ReenactActivity suppliedContext, Uri suppliedThenImage, Uri suppliedNowImage) {
        context = suppliedContext;
        thenImage = suppliedThenImage;
        nowImage = suppliedNowImage;
    }

    public void setThenImage(Uri suppliedThenImage){
        thenImage = suppliedThenImage;
    }

    public void setNowImage(Uri suppliedNowImage){
        nowImage = suppliedNowImage;
    }

    public void setHeader(String suppliedHeader) {
        header = suppliedHeader;
    }

    public void setFooter(String suppliedFooter) {
        footer = suppliedFooter;
    }

    public void setMargins(int top, int right, int bottom, int left, int center) {
        marginPercentTop = top;
        marginPercentRight = right;
        marginPercentBottom = bottom;
        marginPercentLeft = left;
        marginPercentCenter = center;
    }

    public void setBackgroundColor(int suppliedColor){
        backgroundColor = suppliedColor;
    }

    public Bitmap draw() {
        int[] thenImageDimensions = context.getImageDimensions(thenImage);

        int cWidth = thenImageDimensions[0];
        int cHeight = thenImageDimensions[1];

        if (cWidth == 0 || cHeight == 0) {
            context.alert(R.string.error_couldnt_read_original_photo).show();
            return null;
        }

        int[] nowImageDimensions = context.getImageDimensions(nowImage);

        int sWidth = nowImageDimensions[0];
        int sHeight = nowImageDimensions[1];

        if (sWidth == 0 || sHeight == 0) {
            context.alert(R.string.error_couldnt_read_new_photo).show();
            return null;
        }

        int totalHeight;
        int totalWidth;
        float oldRatio;
        float newRatio;

        Rect oldImageDest;
        Rect newImageDest;
        Rect logoDest;

        if (cWidth < cHeight) {
            context.log("Saving combination image in side-by-side format.");

            int smallestHeight = Math.min(1024, Math.min(cHeight, sHeight));
            totalHeight = smallestHeight;

            int newOldHeight = totalHeight;
            int newOldWidth = Math.round(((float) smallestHeight / cHeight) * cWidth);

            int newNewHeight = totalHeight;
            int newNewWidth = Math.round(((float) smallestHeight / sHeight) * sWidth);

            totalWidth = newOldWidth + newNewWidth;

            int marginTop = (int) Math.round((marginPercentTop / 100f) * totalWidth);
            int marginRight = (int) Math.round((marginPercentRight / 100f) * totalWidth);
            int marginBottom = (int) Math.round((marginPercentBottom / 100f) * totalWidth);
            int marginLeft = (int) Math.round((marginPercentLeft / 100f) * totalWidth);
            int marginCenter = (int) Math.round((marginPercentCenter / 100f) * totalWidth);

            context.log("Margins: " + marginTop + ", " + marginRight + ", " + marginBottom + ", " + marginLeft + ", " + marginCenter);

            totalHeight += marginTop + marginBottom;
            totalWidth += marginLeft + marginRight + marginCenter;

            oldRatio = (float) smallestHeight / cHeight;
            newRatio = (float) smallestHeight / sHeight;

            if (context.isRTL()) {
                // RTL languages would expect the "before" shot on the right.
                oldImageDest = new Rect(newNewWidth + marginRight + marginCenter, marginTop, newOldWidth + newNewWidth + marginRight + marginCenter, newOldHeight + marginTop);
                newImageDest = new Rect(marginRight, marginTop, newNewWidth + marginRight, newNewHeight + marginTop);
            }
            else {
                oldImageDest = new Rect(marginLeft, marginTop, marginLeft + newOldWidth, marginTop + newOldHeight);
                newImageDest = new Rect(marginLeft + marginCenter + newOldWidth, marginTop, newOldWidth + newNewWidth + marginLeft + marginCenter, newNewHeight + marginTop);
            }

            int logoWidth = (int) Math.round(totalWidth * 0.04);

            if (context.isRTL()) {
                logoDest = new Rect(
                        marginRight + logoWidth,
                        marginTop + newNewHeight - (logoWidth * 2),
                        marginRight + (2 * logoWidth),
                        marginTop + newNewHeight - logoWidth
                );
            }
            else {
                logoDest = new Rect(
                        marginLeft + newOldWidth + marginCenter + newNewWidth - (logoWidth * 2),
                        marginTop + newNewHeight - (logoWidth * 2),
                        marginLeft + newOldWidth + marginCenter + newNewWidth - logoWidth,
                        marginTop + newNewHeight - logoWidth
                );
            }
        }
        else {
            context.log("Saving combination image in top-to-bottom format.");

            int smallestWidth = Math.min(1024, Math.min(cWidth, sWidth));
            totalWidth = smallestWidth;

            int newOldWidth = totalWidth;
            int newOldHeight = Math.round(((float) smallestWidth / cWidth) * cHeight);

            int newNewWidth = totalWidth;
            int newNewHeight = Math.round(((float) smallestWidth / sWidth) * sHeight);

            totalHeight = newOldHeight + newNewHeight;

            int marginTop = (int) Math.round((marginPercentTop / 100f) * totalWidth);
            int marginRight = (int) Math.round((marginPercentRight / 100f) * totalWidth);
            int marginBottom = (int) Math.round((marginPercentBottom / 100f) * totalWidth);
            int marginLeft = (int) Math.round((marginPercentLeft / 100f) * totalWidth);
            int marginCenter = (int) Math.round((marginPercentCenter / 100f) * totalWidth);

            context.log("Margins: " + marginTop + ", " + marginRight + ", " + marginBottom + ", " + marginLeft + ", " + marginCenter);

            totalHeight += marginTop + marginBottom + marginCenter;
            totalWidth += marginLeft + marginRight;

            oldRatio = (float) smallestWidth / cWidth;
            newRatio = (float) smallestWidth / sWidth;

            oldImageDest = new Rect(marginLeft, marginTop, newOldWidth + marginLeft, newOldHeight + marginTop);
            newImageDest = new Rect(marginLeft, newOldHeight + marginTop + marginCenter, newNewWidth + marginLeft, newOldHeight + newNewHeight + marginTop + marginCenter);

            int logoWidth = (int) Math.round(totalWidth * 0.04);

            if (context.isRTL()) {
                logoDest = new Rect(
                        marginLeft + logoWidth,
                        marginTop + newOldHeight + marginCenter + newNewHeight - (logoWidth * 2),
                        marginLeft + (2 * logoWidth),
                        marginTop + newOldHeight + marginCenter + newNewHeight - logoWidth
                );
            }
            else {
                logoDest = new Rect(
                        marginLeft + newNewWidth - (logoWidth * 2),
                        marginTop + newOldHeight + marginCenter + newNewHeight - (logoWidth * 2),
                        marginLeft + newNewWidth - logoWidth,
                        marginTop + newOldHeight + marginCenter + newNewHeight - logoWidth
                );
            }
        }

        Bitmap cs = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
        cs.eraseColor(backgroundColor);

        Canvas comboImage = new Canvas(cs);

        int oldSampleSize = (int) Math.max(1, Math.floor((float) 1 / oldRatio));
        int newSampleSize = (int) Math.max(1, Math.floor((float) 1 / newRatio));

        context.log("oldRatio:" + oldRatio);
        context.log("newRatio:" + newRatio);

        context.log("oldSampleSize:" + oldSampleSize);
        context.log("newSampleSize:" + newSampleSize);

        BitmapFactory.Options oldOptions = new BitmapFactory.Options();
        oldOptions.inSampleSize = oldSampleSize;

        AssetFileDescriptor oldFileDescriptor;

        try {
            oldFileDescriptor = context.getContentResolver().openAssetFileDescriptor(thenImage, "r");
        } catch (FileNotFoundException e) {
            context.log("File not found", e);

            context.alert(R.string.error_original_photo_missing).show();
            return null;
        }

        BitmapFactory.decodeFileDescriptor(oldFileDescriptor.getFileDescriptor(), null, oldOptions);
        Bitmap oldImage = BitmapFactory.decodeFileDescriptor(oldFileDescriptor.getFileDescriptor(), null, oldOptions);

        float orientation = (float) context.getOrientation(thenImage);

        if (orientation > 0) {
            Matrix rotationMatrix = new Matrix();
            rotationMatrix.postRotate(orientation);
            oldImage = Bitmap.createBitmap(oldImage, 0, 0, oldImage.getWidth(), oldImage.getHeight(), rotationMatrix, true);
        }

        comboImage.drawBitmap(oldImage, new Rect(0, 0, oldImage.getWidth(), oldImage.getHeight()), oldImageDest, null);

        oldImage.recycle();
        oldImage = null;
        oldOptions = null;
        oldFileDescriptor = null;

        BitmapFactory.Options newOptions = new BitmapFactory.Options();
        newOptions.inSampleSize = newSampleSize;

        AssetFileDescriptor newFileDescriptor;

        try {
            newFileDescriptor = context.getContentResolver().openAssetFileDescriptor(nowImage, "r");
        } catch (FileNotFoundException e) {
            context.log("File not found", e);

            context.alert(R.string.error_new_photo_missing).show();

            return null;
        }

        BitmapFactory.decodeFileDescriptor(newFileDescriptor.getFileDescriptor(), null, newOptions);
        Bitmap newImage = BitmapFactory.decodeFileDescriptor(newFileDescriptor.getFileDescriptor(), null, newOptions);

        comboImage.drawBitmap(newImage, new Rect(0, 0, newImage.getWidth(), newImage.getHeight()), newImageDest, null);

        newImage.recycle();
        newImage = null;
        newOptions = null;
        newFileDescriptor = null;

        // Add the Reenact logo to the lower right corner.
        Resources resources = context.getResources();
        Bitmap logoBitmap = BitmapFactory.decodeResource(resources, R.drawable.logo);

        Rect logoSrc = new Rect(0, 0, logoBitmap.getWidth(), logoBitmap.getHeight());

        comboImage.drawBitmap(logoBitmap, logoSrc, logoDest, null);

        return cs;
    }
}
