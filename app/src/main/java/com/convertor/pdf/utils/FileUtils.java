package com.convertor.pdf.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {

    public static File createPdfFile(Context context, String fileName) {
        File pdfDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ConvertorPDF");
        if (!pdfDir.exists()) pdfDir.mkdirs();
        String safeName = fileName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (!safeName.toLowerCase().endsWith(".pdf")) safeName += ".pdf";
        return new File(pdfDir, safeName);
    }

    public static String getFileName(Context context, Uri uri) {
        String displayName = "document";
        try {
            android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) displayName = cursor.getString(nameIndex);
                cursor.close();
            }
        } catch (Exception ignored) {}
        return displayName;
    }

    public static File copyUriToFile(Context context, Uri uri, File dest) throws Exception {
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             FileOutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) os.write(buffer, 0, len);
        }
        return dest;
    }

    public static String getMimeType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "application/msword";
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return "application/vnd.ms-excel";
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) return "application/vnd.ms-powerpoint";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        return "application/octet-stream";
    }

    public static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
