package com.convertor.pdf;

import android.Manifest;
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
        final Activity activity = (Activity) context;

        activity.runOnUiThread(() ->
            Toast.makeText(context, "Buscando actualizaciones...", Toast.LENGTH_SHORT).show()
        );

        new Thread(() -> {
            try {
                URL url = new URL(UPDATE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Cache-Control", "no-cache");

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    showError(activity, "Error de conexion: HTTP " + responseCode);
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONObject json = new JSONObject(sb.toString());
                int latestCode = json.getInt("latestVersionCode");
                String latestName = json.getString("latestVersionName");
                String downloadUrl = json.getString("downloadUrl");
                String changelog = json.optString("changelog", "Nueva version disponible");

                PackageManager pm = context.getPackageManager();
                PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
                int currentCode;
                if (Build.VERSION.SDK_INT >= 28) {
                    currentCode = (int) pInfo.getLongVersionCode();
                } else {
                    currentCode = pInfo.versionCode;
                }

                if (latestCode > currentCode) {
                    showUpdateDialog(activity, latestName, changelog, downloadUrl);
                }

            } catch (Exception e) {
                e.printStackTrace();
                showError(activity, "Error al buscar actualizacion: " + e.getMessage());
            }
        }).start();
    }

    private static void showError(final Activity activity, final String message) {
        activity.runOnUiThread(() ->
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        );
    }

    private static void showUpdateDialog(final Activity activity, String version, String changelog, final String downloadUrl) {
        activity.runOnUiThread(() -> {
            new AlertDialog.Builder(activity)
                .setTitle("\u00a1Actualizacion disponible!")
                .setMessage("Version " + version + "\n\n" + changelog)
                .setCancelable(false)
                .setPositiveButton("Actualizar", (dialog, which) -> {
                    requestNotificationPermission(activity);
                    downloadApk(activity, downloadUrl);
                })
                .setNegativeButton("Mas tarde", null)
                .show();
        });
    }

    private static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            } catch (Exception ignored) {}
        }
    }

    private static void downloadApk(Context context, String downloadUrl) {
        try {
            String fileName = "ConvertorPDF_" + System.currentTimeMillis() + ".apk";

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle("Descargando actualizacion...");
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
                            int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                String path = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                                if (path != null) {
                                    installApk(ctx, Uri.parse(path));
                                }
                            }
                        }
                        c.close();
                        try { ctx.unregisterReceiver(this); } catch (Exception ignored) {}
                    }
                }
            };
            context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error al descargar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void installApk(Context context, Uri apkUri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Error al instalar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
