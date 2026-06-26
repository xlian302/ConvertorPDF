package com.convertor.pdf.converter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.convertor.pdf.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PdfGenerator {

    public static File generateFromBitmaps(Context context, List<Bitmap> bitmaps, String fileName) throws IOException {
        File pdfFile = FileUtils.createPdfFile(context, fileName);
        PdfDocument document = new PdfDocument();

        for (int i = 0; i < bitmaps.size(); i++) {
            Bitmap bmp = bitmaps.get(i);
            int pageWidth = bmp.getWidth();
            int pageHeight = bmp.getHeight();

            if (pageWidth > 2400 || pageHeight > 2400) {
                float scale = Math.min(2400f / pageWidth, 2400f / pageHeight);
                pageWidth = (int) (pageWidth * scale);
                pageHeight = (int) (pageHeight * scale);
                bmp = Bitmap.createScaledBitmap(bmp, pageWidth, pageHeight, true);
            }

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            canvas.drawBitmap(bmp, 0, 0, null);
            document.finishPage(page);
        }

        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            document.writeTo(fos);
        } finally {
            document.close();
        }

        return pdfFile;
    }

    public static File generateFromText(Context context, String text, String fileName) throws IOException {
        File pdfFile = FileUtils.createPdfFile(context, fileName);
        PdfDocument document = new PdfDocument();

        Paint paint = new Paint();
        paint.setTextSize(12);
        paint.setColor(android.graphics.Color.BLACK);
        paint.setAntiAlias(true);

        int pageWidth = 595;
        int pageHeight = 842;
        int margin = 40;
        int textWidth = pageWidth - 2 * margin;
        int lineHeight = (int) (paint.getFontSpacing() + 4);
        int maxLines = (pageHeight - 2 * margin) / lineHeight;

        String[] lines = text.split("\n");
        int lineIndex = 0;

        while (lineIndex < lines.length) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.getPages().size() + 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            int y = margin + lineHeight;
            int drawnLines = 0;
            while (lineIndex < lines.length && drawnLines < maxLines) {
                String line = lines[lineIndex];
                if (line.isEmpty()) {
                    y += lineHeight;
                    drawnLines++;
                    lineIndex++;
                    continue;
                }
                String[] wrapped = wrapText(line, paint, textWidth);
                for (String wrappedLine : wrapped) {
                    if (drawnLines >= maxLines) break;
                    canvas.drawText(wrappedLine, margin, y, paint);
                    y += lineHeight;
                    drawnLines++;
                }
                lineIndex++;
            }
            document.finishPage(page);
        }

        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            document.writeTo(fos);
        } finally {
            document.close();
        }

        return pdfFile;
    }

    private static String[] wrapText(String text, Paint paint, int maxWidth) {
        if (paint.measureText(text) <= maxWidth) return new String[]{text};
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String testLine = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(testLine) > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines.toArray(new String[0]);
    }
}
