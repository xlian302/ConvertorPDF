package com.convertor.pdf.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.convertor.pdf.R;
import com.convertor.pdf.converter.PdfGenerator;
import com.convertor.pdf.utils.ImageFilters;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImageEditActivity extends AppCompatActivity {

    private RecyclerView recyclerImages;
    private ImageView imagePreview;
    private View layoutEditor;
    private com.google.android.material.textfield.TextInputEditText etPdfName;
    private com.google.android.material.button.MaterialButton btnCreatePdf, btnAddImages;
    private com.google.android.material.button.MaterialButton btnRotateLeft, btnRotateRight;
    private com.google.android.material.button.MaterialButton btnFilterOriginal, btnFilterGrayscale, btnFilterSepia, btnFilterInvert;
    private com.google.android.material.button.MaterialButton btnCrop, btnDeleteImage;

    private ImageAdapter adapter;
    private List<ImageItem> images = new ArrayList<>();
    private int selectedIndex = -1;

    public static class ImageItem {
        Uri uri;
        Bitmap originalBitmap;
        Bitmap displayBitmap;
        float rotation;
        Rect cropRect;
        String filter;

        ImageItem(Uri uri) {
            this.uri = uri;
            this.rotation = 0;
            this.cropRect = null;
            this.filter = "original";
        }
    }

    private final ActivityResultLauncher<String> galleryLauncher =
        registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
            if (uris != null) {
                for (Uri uri : uris) {
                    ImageItem item = new ImageItem(uri);
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        item.originalBitmap = BitmapFactory.decodeStream(is);
                        if (is != null) is.close();
                        if (item.originalBitmap != null) {
                            item.displayBitmap = item.originalBitmap;
                            images.add(item);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show();
                    }
                }
                adapter.notifyDataSetChanged();
                if (!images.isEmpty() && selectedIndex < 0) selectImage(0);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_edit);

        recyclerImages = findViewById(R.id.recycler_images);
        imagePreview = findViewById(R.id.image_preview);
        layoutEditor = findViewById(R.id.layout_image_editor);
        etPdfName = findViewById(R.id.et_pdf_name);
        btnCreatePdf = findViewById(R.id.btn_create_pdf);
        btnAddImages = findViewById(R.id.btn_add_images);
        btnRotateLeft = findViewById(R.id.btn_rotate_left);
        btnRotateRight = findViewById(R.id.btn_rotate_right);
        btnFilterOriginal = findViewById(R.id.btn_filter_original);
        btnFilterGrayscale = findViewById(R.id.btn_filter_grayscale);
        btnFilterSepia = findViewById(R.id.btn_filter_sepia);
        btnFilterInvert = findViewById(R.id.btn_filter_invert);
        btnCrop = findViewById(R.id.btn_crop);
        btnDeleteImage = findViewById(R.id.btn_delete_image);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new ImageAdapter(images, new ImageAdapter.Callbacks() {
            @Override public void onItemClick(int position) { selectImage(position); }
            @Override public void onMoveUp(int position) {
                if (position > 0) {
                    ImageItem temp = images.get(position);
                    images.set(position, images.get(position - 1));
                    images.set(position - 1, temp);
                    adapter.notifyItemMoved(position, position - 1);
                    if (selectedIndex == position) selectedIndex = position - 1;
                    else if (selectedIndex == position - 1) selectedIndex = position;
                }
            }
            @Override public void onMoveDown(int position) {
                if (position < images.size() - 1) {
                    ImageItem temp = images.get(position);
                    images.set(position, images.get(position + 1));
                    images.set(position + 1, temp);
                    adapter.notifyItemMoved(position, position + 1);
                    if (selectedIndex == position) selectedIndex = position + 1;
                    else if (selectedIndex == position + 1) selectedIndex = position;
                }
            }
        });

        recyclerImages.setLayoutManager(new LinearLayoutManager(this));
        recyclerImages.setAdapter(adapter);

        btnAddImages.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnCreatePdf.setOnClickListener(v -> createPdf());

        btnRotateLeft.setOnClickListener(v -> {
            if (selectedIndex < 0) return;
            ImageItem item = images.get(selectedIndex);
            item.rotation -= 90;
            updatePreview(item);
        });

        btnRotateRight.setOnClickListener(v -> {
            if (selectedIndex < 0) return;
            ImageItem item = images.get(selectedIndex);
            item.rotation += 90;
            updatePreview(item);
        });

        btnFilterOriginal.setOnClickListener(v -> applyFilter("original"));
        btnFilterGrayscale.setOnClickListener(v -> applyFilter("grayscale"));
        btnFilterSepia.setOnClickListener(v -> applyFilter("sepia"));
        btnFilterInvert.setOnClickListener(v -> applyFilter("invert"));

        btnCrop.setOnClickListener(v -> showCropDialog());

        btnDeleteImage.setOnClickListener(v -> {
            if (selectedIndex < 0) return;
            images.remove(selectedIndex);
            adapter.notifyItemRemoved(selectedIndex);
            if (images.isEmpty()) {
                layoutEditor.setVisibility(View.GONE);
                selectedIndex = -1;
            } else {
                selectImage(Math.min(selectedIndex, images.size() - 1));
            }
        });
    }

    private void selectImage(int index) {
        selectedIndex = index;
        layoutEditor.setVisibility(View.VISIBLE);
        ImageItem item = images.get(index);
        updatePreview(item);
        adapter.setSelectedIndex(index);
    }

    private void updatePreview(ImageItem item) {
        Bitmap working = item.originalBitmap;
        if (item.rotation != 0) {
            working = ImageFilters.rotateBitmap(working, item.rotation);
        }
        if (item.cropRect != null) {
            working = ImageFilters.cropBitmap(working, item.cropRect);
        }
        if (!"original".equals(item.filter)) {
            switch (item.filter) {
                case "grayscale": working = ImageFilters.applyGrayscale(working); break;
                case "sepia": working = ImageFilters.applySepia(working); break;
                case "invert": working = ImageFilters.applyInvert(working); break;
            }
        }
        item.displayBitmap = working;
        imagePreview.setImageBitmap(working);
        adapter.notifyItemChanged(selectedIndex);
    }

    private void applyFilter(String filter) {
        if (selectedIndex < 0) return;
        ImageItem item = images.get(selectedIndex);
        item.filter = filter;
        updatePreview(item);
    }

    private void showCropDialog() {
        if (selectedIndex < 0) return;
        ImageItem item = images.get(selectedIndex);
        View cropView = getLayoutInflater().inflate(R.layout.dialog_crop, null);
        ImageView cropImageView = cropView.findViewById(R.id.crop_image_view);
        cropImageView.setImageBitmap(item.originalBitmap);

        new AlertDialog.Builder(this)
            .setTitle("Recortar imagen")
            .setView(cropView)
            .setPositiveButton("Aceptar", (d, which) -> {
                Toast.makeText(this, "Arrastra para recortar (funcion basica)", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void createPdf() {
        if (images.isEmpty()) {
            Toast.makeText(this, R.string.select_at_least_one, Toast.LENGTH_SHORT).show();
            return;
        }

        String pdfName = etPdfName.getText().toString().trim();
        if (pdfName.isEmpty()) {
            pdfName = "documento_" + System.currentTimeMillis();
        }

        String finalPdfName = pdfName;
        btnCreatePdf.setEnabled(false);
        btnCreatePdf.setText(R.string.processing);

        new Thread(() -> {
            try {
                List<Bitmap> bitmaps = new ArrayList<>();
                for (ImageItem item : images) {
                    Bitmap bmp = item.originalBitmap;
                    if (item.rotation != 0) bmp = ImageFilters.rotateBitmap(bmp, item.rotation);
                    if (item.cropRect != null) bmp = ImageFilters.cropBitmap(bmp, item.cropRect);
                    if (!"original".equals(item.filter)) {
                        switch (item.filter) {
                            case "grayscale": bmp = ImageFilters.applyGrayscale(bmp); break;
                            case "sepia": bmp = ImageFilters.applySepia(bmp); break;
                            case "invert": bmp = ImageFilters.applyInvert(bmp); break;
                        }
                    }
                    bitmaps.add(bmp);
                }

                File resultFile = PdfGenerator.generateFromBitmaps(ImageEditActivity.this, bitmaps, finalPdfName);
                File savedFile = resultFile;
                runOnUiThread(() -> {
                    btnCreatePdf.setEnabled(true);
                    btnCreatePdf.setText(R.string.create_pdf);
                    Toast.makeText(ImageEditActivity.this, "PDF guardado en: " + savedFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    new AlertDialog.Builder(ImageEditActivity.this)
                        .setTitle(R.string.pdf_created)
                        .setMessage("PDF guardado en:\n" + savedFile.getAbsolutePath())
                        .setPositiveButton("Abrir", (d, w) -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.fromFile(savedFile), "application/pdf");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            try { startActivity(intent); } catch (Exception ex) {
                                Toast.makeText(ImageEditActivity.this, "No hay visor de PDF instalado", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cerrar", null)
                        .show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnCreatePdf.setEnabled(true);
                    btnCreatePdf.setText(R.string.create_pdf);
                    Toast.makeText(ImageEditActivity.this, R.string.pdf_error + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
