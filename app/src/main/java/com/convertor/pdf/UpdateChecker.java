package com.convertor.pdf;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private static final String UPDATE_URL = "https://raw.githubusercontent.com/xlian302/ConvertorPDF/main/version.json";

    public static void check(final Context context) {
        new Thread(() -> {
            try {
                URL url = new URL(UPDATE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    showError(context, "Error de conexion: HTTP " + responseCode);
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                int latestCode = json.getInt("latestVersionCode");
                String latestName = json.getString("latestVersionName");
                String downloadUrl = json.getString("downloadUrl");
                String changelog = json.optString("changelog", "Nueva versión disponible");

                PackageManager pm = context.getPackageManager();
                PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
                int currentCode;
                if (Build.VERSION.SDK_INT >= 28) {
                    currentCode = (int) pInfo.getLongVersionCode();
                } else {
                    currentCode = pInfo.versionCode;
                }

                if (latestCode > currentCode) {
                    showUpdateDialog(context, latestName, changelog, downloadUrl);
                }

            } catch (Exception e) {
                e.printStackTrace();
                showError(context, "Error al buscar actualizacion: " + e.getMessage());
            }
        }).start();
    }

    private static void showError(final Context context, final String message) {
        ((Activity) context).runOnUiThread(() ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        );
    }

    private static void showUpdateDialog(final Context context, String version, String changelog, final String downloadUrl) {
        ((Activity) context).runOnUiThread(() -> {
            new AlertDialog.Builder(context)
                    .setTitle("Nueva versi\u00f3n " + version)
                    .setMessage(changelog)
                    .setCancelable(false)
                    .setPositiveButton("Actualizar", (dialog, which) -> downloadApk(context, downloadUrl))
                    .setNegativeButton("M\u00e1s tarde", null)
                    .show();
        });
    }

    private static void downloadApk(Context context, String downloadUrl) {
        String fileName = "ConvertorPDF_v" + System.currentTimeMillis() + ".apk";

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setTitle("Descargando actualizaci\u00f3n...");
        request.setDescription("ConvertorPDF");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = manager.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    Cursor c = manager.query(query);
                    if (c.moveToFirst()) {
                        int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            String path = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                            if (path == null) {
                                path = c.getString(c.getColumnIndex("local_filename"));
                            }
                            if (path != null) {
                                installApk(context, Uri.parse(path));
                            } else {
                                Toast.makeText(context, "Error al obtener el archivo", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    c.close();
                    ctx.unregisterReceiver(this);
                }
            }
        };
        context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private static void installApk(Context context, Uri apkUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }
}
