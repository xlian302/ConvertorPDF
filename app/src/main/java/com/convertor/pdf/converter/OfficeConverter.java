package com.convertor.pdf.converter;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.convertor.pdf.utils.FileUtils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFTextShape;

public class OfficeConverter {

    private static final String TAG = "OfficeConverter";

    public interface ConversionCallback {
        void onProgress(int progress, String status);
        void onComplete(File pdfFile);
        void onError(String error);
    }

    public static void convertToPdf(Context context, Uri fileUri, String pdfName, ConversionCallback callback) {
        new Thread(() -> {
            try {
                ZipSecureFile.setMinInflateRatio(-1.0d);
                String fileName = FileUtils.getFileName(context, fileUri);
                callback.onProgress(10, "Leyendo archivo...");

                File tempFile = new File(context.getCacheDir(), "temp_" + fileName);
                FileUtils.copyUriToFile(context, fileUri, tempFile);

                callback.onProgress(30, "Procesando contenido...");

                String textContent = "";
                String lower = fileName.toLowerCase();
                if (lower.endsWith(".docx")) {
                    textContent = readDocx(tempFile);
                } else if (lower.endsWith(".doc")) {
                    textContent = readDoc(tempFile);
                } else if (lower.endsWith(".xlsx")) {
                    textContent = readXlsx(tempFile);
                } else if (lower.endsWith(".xls")) {
                    textContent = readXls(tempFile);
                } else if (lower.endsWith(".pptx")) {
                    textContent = readPptx(tempFile);
                } else if (lower.endsWith(".ppt")) {
                    textContent = readPpt(tempFile);
                } else {
                    callback.onError("Formato no soportado: " + fileName);
                    return;
                }

                tempFile.delete();
                callback.onProgress(60, "Generando PDF...");

                if (textContent.isEmpty()) textContent = "(El archivo no contiene texto visible)";

                File pdfFile = PdfGenerator.generateFromText(context, textContent, pdfName);

                callback.onProgress(100, "Completado");
                callback.onComplete(pdfFile);

            } catch (Exception e) {
                Log.e(TAG, "Conversion error", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    private static String readDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private static String readDoc(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument doc = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private static String readXlsx(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sb.append("=== ").append(sheet.getSheetName()).append(" ===\n");
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        sb.append(cellToString(cell)).append("\t");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static String readXls(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             HSSFWorkbook workbook = new HSSFWorkbook(fis)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sb.append("=== ").append(sheet.getSheetName()).append(" ===\n");
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        sb.append(cellToString(cell)).append("\t");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static String readPptx(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {
            int slideNum = 1;
            for (XSLFSlide slide : ppt.getSlides()) {
                sb.append("=== Diapositiva ").append(slideNum++).append(" ===\n");
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        sb.append(((XSLFTextShape) shape).getText()).append("\n");
                    }
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static String readPpt(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             HSLFSlideShow ppt = new HSLFSlideShow(fis)) {
            int slideNum = 1;
            for (HSLFSlide slide : ppt.getSlides()) {
                sb.append("=== Diapositiva ").append(slideNum++).append(" ===\n");
                for (HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape) {
                        sb.append(((HSLFTextShape) shape).getText()).append("\n");
                    }
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static String cellToString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: {
                try { return String.valueOf(cell.getNumericCellValue()); } catch (Exception e) {}
                try { return cell.getStringCellValue(); } catch (Exception e) {}
                return cell.getCellFormula();
            }
            default: return "";
        }
    }
}
