package com.ai.middle.platform.service.impl;

import com.ai.middle.platform.common.exception.BusinessException;
import com.ai.middle.platform.service.DocumentProcessService;
import com.aspose.pdf.Document;
import com.aspose.pdf.devices.JpegDevice;
import com.aspose.pdf.devices.Resolution;
import com.aspose.words.SaveFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessServiceImpl implements DocumentProcessService {

    private static final int DEFAULT_RESOLUTION_DPI = 300;

    private static final String OCR_PAGE_OBJECT_TYPE = "kb_document_page";

    private final FileStorageService fileStorageService;

    @Override
    public String convertWordToPdf(File wordFile) {
        try {
            log.info("Converting Word to PDF: {}", wordFile.getAbsolutePath());
            com.aspose.words.Document document = new com.aspose.words.Document(wordFile.getAbsolutePath());
            File pdfFile = File.createTempFile("converted_", ".pdf");
            document.save(pdfFile.getAbsolutePath(), SaveFormat.PDF);
            log.info("Word to PDF conversion succeeded: {}", pdfFile.getAbsolutePath());
            return pdfFile.getAbsolutePath();
        } catch (Exception e) {
            log.error("Failed to convert Word to PDF", e);
            throw new BusinessException("Word to PDF conversion failed: " + e.getMessage());
        }
    }

    @Override
    public List<String> splitPdfToImages(String pdfFilePath, String fileId) {
        List<String> imageUrls = new ArrayList<>();
        Document pdfDocument = null;
        try {
            log.info("Splitting PDF into images: {}", pdfFilePath);
            pdfDocument = new Document(pdfFilePath);
            int pageCount = pdfDocument.getPages().size();
            log.info("PDF page count: {}", pageCount);

            Resolution resolution = new Resolution(DEFAULT_RESOLUTION_DPI);
            JpegDevice jpegDevice = new JpegDevice(resolution);

            for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                File imageFile = File.createTempFile("page_" + pageNumber + "_", ".jpg");
                try (FileOutputStream imageStream = new FileOutputStream(imageFile)) {
                    jpegDevice.process(pdfDocument.getPages().get_Item(pageNumber), imageStream);
                }
                String imageUrl = uploadPageImage(imageFile, fileId, pageNumber);
                imageUrls.add(imageUrl);
                deleteQuietly(imageFile);
                log.info("Processed page {}/{} for file {}", pageNumber, pageCount, fileId);
            }
            return imageUrls;
        } catch (Exception e) {
            log.error("Failed to split PDF into images", e);
            throw new BusinessException("PDF to images conversion failed: " + e.getMessage());
        } finally {
            if (pdfDocument != null) {
                pdfDocument.close();
            }
        }
    }

    @Override
    public String processImageFile(String imageFilePath, String fileId) {
        File imageFile = new File(imageFilePath);
        if (!imageFile.exists()) {
            throw new BusinessException("Image file does not exist: " + imageFilePath);
        }
        try {
            return uploadPageImage(imageFile, fileId, 1);
        } catch (Exception e) {
            log.error("Failed to process image file", e);
            throw new BusinessException("Image processing failed: " + e.getMessage());
        }
    }

    @Override
    public int getPdfPageCount(String pdfFilePath) {
        Document pdfDocument = null;
        try {
            pdfDocument = new Document(pdfFilePath);
            return pdfDocument.getPages().size();
        } catch (Exception e) {
            log.error("Failed to read PDF page count", e);
            throw new BusinessException("Failed to read PDF: " + e.getMessage());
        } finally {
            if (pdfDocument != null) {
                pdfDocument.close();
            }
        }
    }

    private String uploadPageImage(File imageFile, String fileId, int pageNumber) throws IOException {
        try (FileInputStream fis = new FileInputStream(imageFile)) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("fileId", fileId);
            metadata.put("pageNumber", String.valueOf(pageNumber));

            FileInfo fileInfo = fileStorageService.of(fis)
                    .setObjectType(OCR_PAGE_OBJECT_TYPE)
                    .setObjectId(fileId)
                    .setPath("kb/documents/" + fileId + "/pages/")
                    .setSaveFilename("page_" + pageNumber + ".jpg")
                    .setOriginalFilename(imageFile.getName())
                    .setMetadata(metadata)
                    .upload();
            if (fileInfo == null || fileInfo.getUrl() == null) {
                throw new BusinessException("Image upload failed: target URL missing");
            }
            return fileInfo.getUrl();
        }
    }

    private void deleteQuietly(File file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException ex) {
            log.warn("Failed to delete temp file: {}", file.getAbsolutePath(), ex);
        }
    }
}
