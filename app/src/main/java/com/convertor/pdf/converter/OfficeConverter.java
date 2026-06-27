package com.convertor.pdf.converter;

import android.content.Context;
import android.net.Uri;

import com.convertor.pdf.utils.FileUtils;

import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.extractor.OldExcelExtractor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;

public class OfficeConverter {

    public interface ConversionCallback {
        void onProgress(int progress, String status);
        void onComplete(File pdfFile);
        void onError(String error);
    }

    public static void convertToPdf(Context context, Uri fileUri, String pdfName, ConversionCallback callback) {
        new Thread(() -> {
            try {
                String fileName = FileUtils.getFileName(context, fileUri);
                String ext = "";
                int dot = fileName.lastIndexOf('.');
                if (dot > 0) ext = fileName.substring(dot).toLowerCase();

                callback.onProgress(10, "Leyendo archivo...");
                File tmpFile = FileUtils.copyUriToCache(context, fileUri, ext);

                callback.onProgress(30, "Extrayendo texto...");
                String text = extractText(tmpFile, ext);

                callback.onProgress(60, "Generando PDF...");
                File pdfFile = PdfGenerator.generateFromText(context, text, pdfName);

                tmpFile.delete();
                callback.onProgress(100, "Completado");
                callback.onComplete(pdfFile);

            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private static String extractText(File file, String ext) throws Exception {
        switch (ext) {
            case ".docx":
                try (FileInputStream fis = new FileInputStream(file);
                     XWPFDocument doc = new XWPFDocument(fis);
                     XWPFWordExtractor extrator = new XWPFWordExtractor(doc)) {
                    return extrator.getText();
                }
            case ".doc":
                try (FileInputStream fis = new FileInputStream(file);
                     POIFSFileSystem fs = new POIFSFileSystem(fis);
                     org.apache.poi.hwpf.extractor.WordExtractor extractor =
                         new org.apache.poi.hwpf.extractor.WordExtractor(fs)) {
                    return extractor.getText();
                }
            case ".xlsx":
                try (FileInputStream fis = new FileInputStream(file);
                     Workbook wb = WorkbookFactory.create(fis)) {
                    return extractExcelText(wb);
                }
            case ".xls":
                try (FileInputStream fis = new FileInputStream(file);
                     Workbook wb = new HSSFWorkbook(fis)) {
                    return extractExcelText(wb);
                }
            case ".pptx":
                try (FileInputStream fis = new FileInputStream(file);
                     XMLSlideShow ppt = new XMLSlideShow(fis)) {
                    StringBuilder sb = new StringBuilder();
                    for (XSLFSlide slide : ppt.getSlides()) {
                        for (var shape : slide.getShapes()) {
                            if (shape instanceof XSLFTextShape) {
                                if (sb.length() > 0) sb.append("\n");
                                sb.append(((XSLFTextShape) shape).getText());
                            }
                        }
                        sb.append("\n---\n");
                    }
                    return sb.toString();
                }
            case ".ppt":
                try (FileInputStream fis = new FileInputStream(file);
                     HSLFSlideShow ppt = new HSLFSlideShow(fis)) {
                    StringBuilder sb = new StringBuilder();
                    for (var slide : ppt.getSlides()) {
                        for (var shape : slide.getShapes()) {
                            if (shape instanceof org.apache.poi.hslf.usermodel.HSLFTextShape) {
                                if (sb.length() > 0) sb.append("\n");
                                sb.append(((org.apache.poi.hslf.usermodel.HSLFTextShape) shape).getText());
                            }
                        }
                        sb.append("\n---\n");
                    }
                    return sb.toString();
                }
            default:
                throw new Exception("Formato no soportado: " + ext);
        }
    }

    private static String extractExcelText(Workbook wb) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet sheet = wb.getSheetAt(i);
            if (sb.length() > 0) sb.append("\n=== ").append(sheet.getSheetName()).append(" ===\n");
            for (Row row : sheet) {
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null) {
                        if (c > 0) sb.append("\t");
                        switch (cell.getCellType()) {
                            case STRING: sb.append(cell.getStringCellValue()); break;
                            case NUMERIC: sb.append(cell.getNumericCellValue()); break;
                            case BOOLEAN: sb.append(cell.getBooleanCellValue()); break;
                            case FORMULA:
                                try { sb.append(cell.getNumericCellValue()); }
                                catch (Exception e) { sb.append(cell.getStringCellValue()); }
                                break;
                            default: sb.append("");
                        }
                    }
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
