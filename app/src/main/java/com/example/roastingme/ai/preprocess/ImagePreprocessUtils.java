package com.example.roastingme.ai.preprocess;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;

import com.example.roastingme.ai.model.ImagePreprocessResult;

import java.io.InputStream;

public class ImagePreprocessUtils {

    public static final int AI_INPUT_SIZE = 512;
    private static final int MAX_PREVIEW_SIZE = 1080;

    /**
     * 이미지 Uri로부터 안전하게 디코딩, 회전 보정 및 AI 표준 비트맵 생성을 수행합니다.
     */
    public static ImagePreprocessResult processImage(Context context, Uri imageUri) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 메모리 절약을 위한 inSampleSize 계산 및 디코딩
            Bitmap sampledBitmap = decodeSampledBitmapFromUri(context, imageUri, MAX_PREVIEW_SIZE, MAX_PREVIEW_SIZE);
            if (sampledBitmap == null) return null;

            // 2. EXIF 메타데이터 회전각 보정
            Bitmap rotatedBitmap = rotateImageIfRequired(context, sampledBitmap, imageUri);

            int origWidth = rotatedBitmap.getWidth();
            int origHeight = rotatedBitmap.getHeight();

            // 3. AI 모델 입력용 Scaled Bitmap (512x512) 생성
            Bitmap aiInputBitmap = Bitmap.createScaledBitmap(rotatedBitmap, AI_INPUT_SIZE, AI_INPUT_SIZE, true);

            long elapsedTime = System.currentTimeMillis() - startTime;

            return new ImagePreprocessResult(
                    rotatedBitmap,
                    aiInputBitmap,
                    origWidth,
                    origHeight,
                    elapsedTime
            );

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap decodeSampledBitmapFromUri(Context context, Uri uri, int reqWidth, int reqHeight) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(is, null, options);
        }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        try (InputStream is2 = context.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(is2, null, options);
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws Exception {
        try (InputStream input = context.getContentResolver().openInputStream(selectedImage)) {
            if (input == null) return img;

            ExifInterface ei = new ExifInterface(input);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        if (img != rotatedImg) {
            img.recycle();
        }
        return rotatedImg;
    }
}