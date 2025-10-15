package com.pdfprocessor.controller;

import com.pdfprocessor.entity.Document;
import com.pdfprocessor.service.FileService;
import com.pdfprocessor.service.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;
    
    @Autowired
    private AnalysisService analysisService;

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        try {
            Document document = fileService.getDocumentByFileName(fileName);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            return serveFile(document.getFilePath(), fileName, MediaType.APPLICATION_PDF);
        } catch (RuntimeException e) {
            logger.error("Error serving file: {}", fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/analysis/{documentId}/{analysisType}/{pageNumber}")
    public ResponseEntity<Resource> getAnalysisResultImage(
            @PathVariable String documentId,
            @PathVariable String analysisType,
            @PathVariable int pageNumber) {
        try {
            UUID docId = UUID.fromString(documentId);
            List<com.pdfprocessor.entity.AnalysisFile> results = 
                analysisService.getAnalysisResults(docId, analysisType);
            
            com.pdfprocessor.entity.AnalysisFile pageResult = results.stream()
                .filter(result -> result.getPageNumber() == pageNumber)
                .findFirst()
                .orElse(null);
            
            if (pageResult == null) {
                return ResponseEntity.notFound().build();
            }
            
            return serveFile(pageResult.getResultFilePath(), 
                           Paths.get(pageResult.getResultFilePath()).getFileName().toString(), 
                           MediaType.IMAGE_PNG);
        } catch (RuntimeException e) {
            logger.error("Error serving analysis result image: documentId={}, analysisType={}, pageNumber={}", 
                        documentId, analysisType, pageNumber, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/analysis/{documentId}/{analysisType}/pdf/{fileName}")
    public ResponseEntity<Resource> getAnalysisPdf(@PathVariable String documentId, 
                                                  @PathVariable String analysisType, 
                                                  @PathVariable String fileName) {
        try {
            // Construct the PDF file path
            String pdfFilePath = "uploads/analysis/" + documentId + "/" + analysisType + "/" + fileName;
            Path path = Paths.get(pdfFilePath);
            File file = path.toFile();
            
            if (!file.exists()) {
                logger.warn("Analysis PDF not found: {}", pdfFilePath);
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error serving analysis PDF: documentId={}, analysisType={}, fileName={}", 
                        documentId, analysisType, fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private ResponseEntity<Resource> serveFile(String filePath, String fileName, MediaType contentType) {
        try {
            Path path = Paths.get(filePath);
            File file = path.toFile();
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .contentType(contentType)
                    .body(resource);
        } catch (RuntimeException e) {
            logger.error("Error serving file: {}", filePath, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
