package com.convertor.pdf.utils;

import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    public static File createPdfFile(Context context, String fileName) throws IOException {
        File dir = new File(context.getExternalFilesDir(null), "PDFs");
        if (!dir.exists()) dir.mkdirs();
        String name = fileName.endsWith(".pdf") ? fileName : fileName + ".pdf";
        File file = new File(dir, name);
        int counter = 1;
        while (file.exists()) {
            String base = fileName.endsWith(".pdf") ? fileName.substring(0, fileName.length() - 4) : fileName;
            file = new File(dir, base + "_" + counter + ".pdf");
            counter++;
        }
        file.createNewFile();
        return file;
    }

    public static String getFileName(Context context, Uri uri) {
        String name = "documento";
        try (var cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        } catch (Exception ignored) {}
        if (name == null || name.isEmpty()) name = "documento";
        return name;
    }

    public static File copyUriToCache(Context context, Uri uri, String extension) throws IOException {
        InputStream is = context.getContentResolver().openInputStream(uri);
        if (is == null) throw new IOException("No se pudo abrir el archivo");
        File tmp = File.createTempFile("office_", extension, context.getCacheDir());
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) fos.write(buf, 0, len);
        }
        is.close();
        return tmp;
    }
}
