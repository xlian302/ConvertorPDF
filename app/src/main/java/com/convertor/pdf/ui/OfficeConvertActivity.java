package com.convertor.pdf.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
    private com.google.android.material.button.MaterialButton btnSelectFile, btnConvert;
    private ProgressBar progressBar;
    private TextView textStatus;
    private TextView toolbarTitle;

    private final ActivityResultLauncher<String> fileLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedFileUri = uri;
                String fileName = FileUtils.getFileName(this, uri);
                toolbarTitle.setText(fileName);
                btnConvert.setEnabled(true);
                Toast.makeText(this, "Archivo seleccionado: " + fileName, Toast.LENGTH_SHORT).show();
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
        btnSelectFile = findViewById(R.id.btn_select_file);
        btnConvert = findViewById(R.id.btn_convert);
        etPdfName = findViewById(R.id.et_pdf_name);
        progressBar = findViewById(R.id.progress_bar);
        textStatus = findViewById(R.id.text_status);

        String title;
        String mimeType;
        switch (type) {
            case "excel":
                title = "Excel a PDF";
                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                break;
            case "ppt":
                title = "PowerPoint a PDF";
                mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                break;
            default:
                title = "Word a PDF";
                mimeType = "application/msword";
                break;
        }
        toolbarTitle.setText(title);

        btnSelectFile.setOnClickListener(v -> fileLauncher.launch(mimeType + "|*/*"));

        btnConvert.setOnClickListener(v -> {
            if (selectedFileUri == null) return;
            String pdfName = etPdfName.getText().toString().trim();
            if (pdfName.isEmpty()) pdfName = "documento_" + System.currentTimeMillis();

            btnConvert.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            textStatus.setVisibility(View.VISIBLE);

            OfficeConverter.convertToPdf(this, selectedFileUri, pdfName, new OfficeConverter.ConversionCallback() {
                @Override
                public void onProgress(int progress, String status) {
                    runOnUiThread(() -> {
                        progressBar.setProgress(progress);
                        textStatus.setText(status);
                    });
                }

                @Override
                public void onComplete(File pdfFile) {
                    runOnUiThread(() -> {
                        btnConvert.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        textStatus.setVisibility(View.GONE);
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
                        progressBar.setVisibility(View.GONE);
                        textStatus.setVisibility(View.GONE);
                        Toast.makeText(OfficeConvertActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }
}
