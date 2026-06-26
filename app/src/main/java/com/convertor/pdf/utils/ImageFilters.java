package com.convertor.pdf.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

public class ImageFilters {

    public static Bitmap applyOriginal(Bitmap source) {
        return source;
    }

    public static Bitmap applyGrayscale(Bitmap source) {
        Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(source, 0, 0, paint);
        return result;
    }

    public static Bitmap applySepia(Bitmap source) {
        Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix(new float[]{
            0.393f, 0.769f, 0.189f, 0, 0,
            0.349f, 0.686f, 0.168f, 0, 0,
            0.272f, 0.534f, 0.131f, 0, 0,
            0, 0, 0, 1, 0
        });
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(source, 0, 0, paint);
        return result;
    }

    public static Bitmap applyInvert(Bitmap source) {
        Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix(new float[]{
            -1, 0, 0, 0, 255,
            0, -1, 0, 0, 255,
            0, 0, -1, 0, 255,
            0, 0, 0, 1, 0
        });
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(source, 0, 0, paint);
        return result;
    }

    public static Bitmap applyBrightness(Bitmap source, float brightness) {
        Bitmap result = source.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix(new float[]{
            1, 0, 0, 0, brightness,
            0, 1, 0, 0, brightness,
            0, 0, 1, 0, brightness,
            0, 0, 0, 1, 0
        });
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(source, 0, 0, paint);
        return result;
    }

    public static Bitmap rotateBitmap(Bitmap source, float degrees) {
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap cropBitmap(Bitmap source, android.graphics.Rect rect) {
        int x = Math.max(0, rect.left);
        int y = Math.max(0, rect.top);
        int width = Math.min(rect.width(), source.getWidth() - x);
        int height = Math.min(rect.height(), source.getHeight() - y);
        if (width <= 0 || height <= 0) return source;
        return Bitmap.createBitmap(source, x, y, width, height);
    }
}
