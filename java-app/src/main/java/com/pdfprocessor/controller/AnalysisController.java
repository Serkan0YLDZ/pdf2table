package com.pdfprocessor.controller;

import com.pdfprocessor.entity.AnalysisFile;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for PDF analysis operations
 */
@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
public class AnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisController.class);

    @Autowired
    private AnalysisService analysisService;

    /**
     * Start analysis for a document
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startAnalysis(@RequestBody Map<String, Object> request) {
        try {
            String documentIdStr = (String) request.get("documentId");
            String analysisType = (String) request.get("analysisType");

            if (documentIdStr == null || analysisType == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "documentId and analysisType are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            UUID documentId = UUID.fromString(documentIdStr);
            analysisService.startAnalysis(documentId, analysisType);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Analysis started successfully");
            response.put("documentId", documentId);
            response.put("analysisType", analysisType);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid request parameters", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("Error starting analysis", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to start analysis: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get analysis results for a document and analysis type
     */
    @GetMapping("/results/{documentId}/{analysisType}")
    public ResponseEntity<List<AnalysisFile>> getAnalysisResults(
            @PathVariable UUID documentId,
            @PathVariable String analysisType) {
        try {
            logger.info("Getting analysis results for document: {} type: {}", documentId, analysisType);
            
            List<AnalysisFile> results = analysisService.getAnalysisResults(documentId, analysisType);
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            logger.error("Error getting analysis results", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get analysis status for a document and analysis type
     */
    @GetMapping("/status/{documentId}/{analysisType}")
    public ResponseEntity<Map<String, Object>> getAnalysisStatus(
            @PathVariable UUID documentId,
            @PathVariable String analysisType) {
        try {
            logger.info("Getting analysis status for document: {} type: {}", documentId, analysisType);
            
            Map<String, Object> status = analysisService.getAnalysisStatus(documentId, analysisType);
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Error getting analysis status", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get analysis status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get analysis result file (image)
     */
    @GetMapping("/files/{analysisFileId}")
    public ResponseEntity<Resource> getAnalysisFile(@PathVariable UUID analysisFileId) {
        try {
            logger.info("Getting analysis file: {}", analysisFileId);
            
            // This would need to be implemented in AnalysisService
            // For now, we'll return a placeholder response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Analysis file endpoint not yet implemented");
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("Error getting analysis file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get analysis result file by document ID, analysis type and page number
     */
    @GetMapping("/files/{documentId}/{analysisType}/{pageNumber}")
    public ResponseEntity<Resource> getAnalysisFileByPage(
            @PathVariable UUID documentId,
            @PathVariable String analysisType,
            @PathVariable Integer pageNumber) {
        try {
            logger.info("Getting analysis file for document: {} type: {} page: {}", 
                       documentId, analysisType, pageNumber);
            
            List<AnalysisFile> results = analysisService.getAnalysisResults(documentId, analysisType);
            
            AnalysisFile targetFile = results.stream()
                .filter(file -> file.getPageNumber().equals(pageNumber))
                .findFirst()
                .orElse(null);

            if (targetFile == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(targetFile.getResultFilePath());
            File file = filePath.toFile();
            
            if (!file.exists()) {
                logger.warn("Analysis result file not found: {}", targetFile.getResultFilePath());
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "inline; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("Error getting analysis file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Poll analysis results from Python service
     */
    @PostMapping("/poll/{documentId}/{analysisType}")
    public ResponseEntity<Map<String, Object>> pollAnalysisResults(
            @PathVariable UUID documentId,
            @PathVariable String analysisType) {
        try {
            logger.info("Polling analysis results for document: {} type: {}", documentId, analysisType);
            
            analysisService.pollAndSaveAnalysisResults(documentId, analysisType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Analysis results polled successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error polling analysis results", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to poll analysis results: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "AnalysisController");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}
