package com.convertor.pdf.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.convertor.pdf.R;
import com.convertor.pdf.converter.OfficeConverter;
import com.convertor.pdf.utils.FileUtils;

import java.io.File;

public class OfficeConvertActivity extends AppCompatActivity {

    private String type;
    private Uri selectedFileUri;
    private com.google.android.material.textfield.TextInputEditText etPdfName;
    private com.google.android.material.button.MaterialButton btnConvert;
    private ProgressBar progressBar;
    private TextView textProgressStatus;
    private TextView toolbarTitle;
    private View cardDropZone, cardFileInfo, cardProgress;
    private ImageView icFileType;
    private TextView textFileName, textFileSize;
    private com.google.android.material.button.MaterialButton btnRemoveFile;

    private final ActivityResultLauncher<String> fileLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedFileUri = uri;
                String fileName = FileUtils.getFileName(this, uri);
                long fileSize = FileUtils.getFileSize(this, uri);
                showFileInfo(fileName, fileSize);
                btnConvert.setEnabled(true);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_office_convert);

        type = getIntent().getStringExtra("type");
        if (type == null) type = "word";

        toolbarTitle = findViewById(R.id.toolbar_title);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        cardDropZone = findViewById(R.id.layout_drop_zone);
        cardFileInfo = findViewById(R.id.card_file_info);
        cardProgress = findViewById(R.id.card_progress);
        icFileType = findViewById(R.id.ic_file_type);
        textFileName = findViewById(R.id.text_file_name);
        textFileSize = findViewById(R.id.text_file_size);
        btnRemoveFile = findViewById(R.id.btn_remove_file);
        btnConvert = findViewById(R.id.btn_convert);
        etPdfName = findViewById(R.id.et_pdf_name);
        progressBar = findViewById(R.id.progress_bar);
        textProgressStatus = findViewById(R.id.text_progress_status);

        String title;
        String mimeType;
        switch (type) {
            case "excel":
                title = "Excel a PDF";
                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                icFileType.setImageResource(R.drawable.ic_file_excel);
                break;
            case "ppt":
                title = "PowerPoint a PDF";
                mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                icFileType.setImageResource(R.drawable.ic_file_ppt);
                break;
            default:
                title = "Word a PDF";
                mimeType = "application/msword";
                icFileType.setImageResource(R.drawable.ic_file_word);
                break;
        }
        toolbarTitle.setText(title);

        cardDropZone.setOnClickListener(v -> fileLauncher.launch(mimeType + "|*/*"));

        btnRemoveFile.setOnClickListener(v -> {
            selectedFileUri = null;
            cardFileInfo.setVisibility(View.GONE);
            cardDropZone.setVisibility(View.VISIBLE);
            btnConvert.setEnabled(false);
        });

        btnConvert.setOnClickListener(v -> {
            if (selectedFileUri == null) return;
            String pdfName = etPdfName.getText().toString().trim();
            if (pdfName.isEmpty()) pdfName = "documento_" + System.currentTimeMillis();

            btnConvert.setEnabled(false);
            cardProgress.setVisibility(View.VISIBLE);
            textProgressStatus.setText(R.string.processing);

            OfficeConverter.convertToPdf(this, selectedFileUri, pdfName, new OfficeConverter.ConversionCallback() {
                @Override
                public void onProgress(int progress, String status) {
                    runOnUiThread(() -> {
                        progressBar.setProgress(progress);
                        textProgressStatus.setText(status);
                    });
                }

                @Override
                public void onComplete(File pdfFile) {
                    runOnUiThread(() -> {
                        btnConvert.setEnabled(true);
                        cardProgress.setVisibility(View.GONE);
                        new AlertDialog.Builder(OfficeConvertActivity.this)
                            .setTitle(R.string.pdf_created)
                            .setMessage("PDF guardado en:\n" + pdfFile.getAbsolutePath())
                            .setPositiveButton("Abrir", (d, w) -> {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(pdfFile), "application/pdf");
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                try { startActivity(intent); } catch (Exception e) {
                                    Toast.makeText(OfficeConvertActivity.this, "No hay visor de PDF", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cerrar", null)
                            .show();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        btnConvert.setEnabled(true);
                        cardProgress.setVisibility(View.GONE);
                        Toast.makeText(OfficeConvertActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private void showFileInfo(String fileName, long fileSize) {
        cardDropZone.setVisibility(View.GONE);
        cardFileInfo.setVisibility(View.VISIBLE);
        textFileName.setText(fileName);
        String sizeStr;
        if (fileSize < 1024) {
            sizeStr = fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            sizeStr = String.format("%.1f KB", fileSize / 1024.0);
        } else {
            sizeStr = String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
        textFileSize.setText(sizeStr);
    }
}
