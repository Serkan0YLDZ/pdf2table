package com.pdfprocessor.service;

import com.pdfprocessor.entity.AnalysisFile;
import com.pdfprocessor.entity.Document;
import com.pdfprocessor.repository.AnalysisFileRepository;
import com.pdfprocessor.repository.DocumentRepository;
import com.pdfprocessor.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling PDF analysis operations
 */
@Service
public class AnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisService.class);

    private final AnalysisFileRepository analysisFileRepository;
    private final DocumentRepository documentRepository;
    private final RestTemplate restTemplate;

    @Value("${analysis.python.service.url:http://localhost:8000}")
    private String pythonServiceUrl;

    @Value("${analysis.results.dir:./analysis_results}")
    private String analysisResultsDir;

    @Value("${analysis.timeout:300000}")
    private int analysisTimeout;

    @Autowired
    public AnalysisService(AnalysisFileRepository analysisFileRepository, 
                          DocumentRepository documentRepository) {
        this.analysisFileRepository = analysisFileRepository;
        this.documentRepository = documentRepository;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Start analysis for a document
     */
    @Transactional
    public void startAnalysis(UUID documentId, String analysisType) {
        logger.info("Starting analysis for document: {} with type: {}", documentId, analysisType);

        // Find document
        Optional<Document> documentOpt = documentRepository.findById(documentId);
        if (documentOpt.isEmpty()) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        Document document = documentOpt.get();

        // Validate analysis type
        if (!isValidAnalysisType(analysisType)) {
            throw new IllegalArgumentException("Invalid analysis type: " + analysisType);
        }

        // Update document analysis status
        updateDocumentAnalysisStatus(document, analysisType, "IN_PROGRESS");

        // Prepare request for Python service
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("document_id", documentId.toString());
        requestBody.put("analysis_type", analysisType);
        requestBody.put("file_path", document.getFilePath());
        requestBody.put("file_name", document.getFileName());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            // Call Python service - sadece bu satırı try-catch'e al
            String url = pythonServiceUrl + "/analyze";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Python service returned error status: {}", response.getStatusCode());
                updateDocumentAnalysisStatus(document, analysisType, "FAILED");
                throw new RuntimeException("Analysis service returned error: " + response.getStatusCode());
            }
            
            logger.info("Analysis started successfully for document: {}", documentId);

        } catch (RestClientException e) {
            logger.error("Failed to connect to Python analysis service for document: {}", documentId, e);
            updateDocumentAnalysisStatus(document, analysisType, "FAILED");
            throw new RuntimeException("Analysis service is unavailable: " + e.getMessage());
        }
    }

    /**
     * Get analysis results for a document and analysis type
     */
    public List<AnalysisFile> getAnalysisResults(UUID documentId, String analysisType) {
        logger.info("Getting analysis results for document: {} with type: {}", documentId, analysisType);

        List<AnalysisFile> results = analysisFileRepository
            .findByDocumentIdAndAnalysisTypeOrderByPageNumber(documentId, analysisType);

        // Verify file existence
        results.removeIf(result -> {
            Path filePath = Paths.get(result.getResultFilePath());
            if (!Files.exists(filePath)) {
                logger.warn("Analysis result file not found: {}", result.getResultFilePath());
                return true;
            }
            return false;
        });

        return results;
    }

    /**
     * Check if analysis is complete for a document and analysis type
     */
    public boolean isAnalysisComplete(UUID documentId, String analysisType) {
        logger.debug("Checking analysis completion for document: {} with type: {}", documentId, analysisType);

        Optional<Document> documentOpt = documentRepository.findById(documentId);
        if (documentOpt.isEmpty()) {
            return false;
        }

        Document document = documentOpt.get();
        Map<String, Object> analysisResults = getAnalysisResultsMap(document);

        if (analysisResults == null) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> typeResults = (Map<String, Object>) analysisResults.get(analysisType);
        if (typeResults == null) {
            return false;
        }

        String status = (String) typeResults.get("status");
        return "COMPLETED".equals(status);
    }

    /**
     * Get analysis status information
     */
    public Map<String, Object> getAnalysisStatus(UUID documentId, String analysisType) {
        logger.debug("Getting analysis status for document: {} with type: {}", documentId, analysisType);

        Map<String, Object> status = new HashMap<>();
        
        long processedPages = analysisFileRepository.countByDocumentIdAndAnalysisType(documentId, analysisType);
        Integer maxPage = analysisFileRepository.findMaxPageNumberByDocumentIdAndAnalysisType(documentId, analysisType);
        
        status.put("processedPages", processedPages);
        status.put("maxPage", maxPage != null ? maxPage : 0);
        status.put("complete", isAnalysisComplete(documentId, analysisType));

        return status;
    }

    /**
     * Save analysis result file
     */
    @Transactional
    public void saveAnalysisResult(UUID documentId, String analysisType, int pageNumber, String filePath) {
        logger.info("Saving analysis result for document: {} type: {} page: {}", documentId, analysisType, pageNumber);

        Optional<Document> documentOpt = documentRepository.findById(documentId);
        if (documentOpt.isEmpty()) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }

        Document document = documentOpt.get();

        // Check if result already exists
        if (analysisFileRepository.existsByDocumentIdAndAnalysisTypeAndPageNumber(documentId, analysisType, pageNumber)) {
            logger.warn("Analysis result already exists for document: {} type: {} page: {}", documentId, analysisType, pageNumber);
            return;
        }

        // Calculate file size
        long fileSize = 0;
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            try {
                fileSize = Files.size(path);
            } catch (Exception e) {
                logger.warn("Could not calculate file size for: {}", filePath, e);
            }
        }

        // Create and save analysis file
        AnalysisFile analysisFile = new AnalysisFile(document, analysisType, pageNumber, filePath);
        analysisFile.setFileSize(fileSize);
        analysisFileRepository.save(analysisFile);

        logger.info("Analysis result saved successfully: {}", analysisFile.getId());
    }

    /**
     * Update document analysis status
     */
    @Transactional
    public void updateDocumentAnalysisStatus(Document document, String analysisType, String status) {
        logger.info("Updating analysis status for document: {} type: {} status: {}", 
                   document.getId(), analysisType, status);

        Map<String, Object> analysisResults = getAnalysisResultsMap(document);
        if (analysisResults == null) {
            analysisResults = new HashMap<>();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> typeResults = (Map<String, Object>) analysisResults.get(analysisType);
        if (typeResults == null) {
            typeResults = new HashMap<>();
        }

        typeResults.put("status", status);
        typeResults.put("lastUpdated", System.currentTimeMillis());
        analysisResults.put(analysisType, typeResults);

        document.setAnalysisResults(analysisResults);
        documentRepository.save(document);
    }

    /**
     * Get analysis results map from document
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getAnalysisResultsMap(Document document) {
        Object analysisResults = document.getAnalysisResults();
        if (analysisResults instanceof Map) {
            return (Map<String, Object>) analysisResults;
        }
        return null;
    }

    /**
     * Poll Python service for analysis results and save them to database
     */
    @Transactional
    public void pollAndSaveAnalysisResults(UUID documentId, String analysisType) {
        logger.info("Polling analysis results for document: {} type: {}", documentId, analysisType);
        
        try {
            // Call Python service to get results
            String url = pythonServiceUrl + "/results/" + documentId + "/" + analysisType;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");
                
                if (results != null && !results.isEmpty()) {
                    logger.info("Found {} analysis results for document: {} type: {}", 
                               results.size(), documentId, analysisType);
                    
                    // Save each result to database
                    for (Map<String, Object> result : results) {
                        Integer pageNumber = (Integer) result.get("page_number");
                        String filePath = (String) result.get("file_path");
                        
                        if (pageNumber != null && filePath != null) {
                            saveAnalysisResult(documentId, analysisType, pageNumber, filePath);
                        }
                    }
                    
                    // Update document status to completed
                    Optional<Document> documentOpt = documentRepository.findById(documentId);
                    if (documentOpt.isPresent()) {
                        updateDocumentAnalysisStatus(documentOpt.get(), analysisType, "COMPLETED");
                    }
                }
            } else {
                logger.warn("Python service returned non-success status: {} for document: {} type: {}", 
                           response.getStatusCode(), documentId, analysisType);
            }
            
        } catch (RestClientException e) {
            logger.error("Failed to connect to Python service for polling results: document: {} type: {}", 
                        documentId, analysisType, e);
        }
    }

    /**
     * Validate analysis type
     */
    private boolean isValidAnalysisType(String analysisType) {
        return "deepdoctection".equals(analysisType) || "docling".equals(analysisType);
    }

    /**
     * Get file size in human readable format
     */
    public String getFileSizeInHumanReadable(Long fileSize) {
        return FileUtils.getFileSizeInHumanReadable(fileSize);
    }
}
