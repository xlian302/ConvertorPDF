package com.convertor.pdf.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;

public class ImageFilters {

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap cropBitmap(Bitmap source, Rect rect) {
        return Bitmap.createBitmap(source, rect.left, rect.top, rect.width(), rect.height());
    }

    public static Bitmap applyGrayscale(Bitmap source) {
        Bitmap result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig() != null ? source.getConfig() : Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(source, 0, 0, paint);
        return result;
    }

    public static Bitmap applySepia(Bitmap source) {
        Bitmap result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig() != null ? source.getConfig() : Bitmap.Config.ARGB_8888);
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
        Bitmap result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig() != null ? source.getConfig() : Bitmap.Config.ARGB_8888);
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
}
