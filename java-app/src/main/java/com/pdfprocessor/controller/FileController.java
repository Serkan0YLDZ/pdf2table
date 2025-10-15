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
            // Find document by filename
            Document document = fileService.getDocumentByFileName(fileName);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(document.getFilePath());
            File file = filePath.toFile();
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);
                    
        } catch (Exception e) {
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
            
            // Get analysis results
            List<com.pdfprocessor.entity.AnalysisFile> results = 
                analysisService.getAnalysisResults(docId, analysisType);
            
            // Find the specific page result
            com.pdfprocessor.entity.AnalysisFile pageResult = results.stream()
                .filter(result -> result.getPageNumber() == pageNumber)
                .findFirst()
                .orElse(null);
            
            if (pageResult == null) {
                return ResponseEntity.notFound().build();
            }
            
            Path filePath = Paths.get(pageResult.getResultFilePath());
            File file = filePath.toFile();
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("Error serving analysis result image: documentId={}, analysisType={}, pageNumber={}", 
                        documentId, analysisType, pageNumber, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
