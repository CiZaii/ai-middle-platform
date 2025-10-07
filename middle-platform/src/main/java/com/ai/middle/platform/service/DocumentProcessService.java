package com.ai.middle.platform.service;

import java.io.File;
import java.util.List;

/**
 * Document processing service for file conversion and page extraction.
 */
public interface DocumentProcessService {

    /**
     * Convert Word document to PDF.
     *
     * @param wordFile Word file (DOCX)
     * @return PDF file path
     */
    String convertWordToPdf(File wordFile);

    /**
     * Split PDF into page images.
     *
     * @param pdfFilePath PDF file path
     * @param fileId      File ID for storage path
     * @return List of image URLs
     */
    List<String> splitPdfToImages(String pdfFilePath, String fileId);

    /**
     * Process image file (single page).
     *
     * @param imageFilePath Image file path
     * @param fileId        File ID for storage path
     * @return Image URL
     */
    String processImageFile(String imageFilePath, String fileId);

    /**
     * Get total page count of PDF.
     *
     * @param pdfFilePath PDF file path
     * @return number of pages
     */
    int getPdfPageCount(String pdfFilePath);
}
