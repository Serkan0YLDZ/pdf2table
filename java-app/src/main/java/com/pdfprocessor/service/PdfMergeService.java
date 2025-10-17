package com.pdfprocessor.service;

import com.pdfprocessor.entity.AnalysisFile;
import com.pdfprocessor.entity.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for merging analysis images into PDF format
 */
@Service
public class PdfMergeService {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfMergeService.class);
    
    /**
     * Merge analysis images into a single PDF using Python script
     */
    public byte[] mergeAnalysisImagesToPdf(Document document, String analysisType, List<AnalysisFile> analysisFiles) {
        try {
            // Use Python script to create PDF
            String pdfPath = createAnalysisPdfWithPython(document.getId().toString(), analysisType);
            
            if (pdfPath == null) {
                throw new RuntimeException("Failed to create PDF using Python script");
            }
            
            // Read the created PDF file
            Path pdfFilePath = Paths.get(pdfPath);
            byte[] pdfBytes = Files.readAllBytes(pdfFilePath);
            
            logger.info("Successfully created PDF with {} bytes for document {} using Python script", 
                       pdfBytes.length, document.getId());
            
            return pdfBytes;
            
        } catch (Exception e) {
            logger.error("Error merging analysis images to PDF for document {}: {}", 
                        document.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to merge analysis images to PDF", e);
        }
    }
    
    /**
     * Create analysis PDF using Python script
     */
    private String createAnalysisPdfWithPython(String documentId, String analysisType) {
        // Path to Python script
        String pythonScriptPath = "../python-analysis-service/docling_to_pdf.py";
        Path scriptPath = Paths.get(pythonScriptPath).toAbsolutePath();
        
        if (!Files.exists(scriptPath)) {
            logger.error("Python script not found at: {}", scriptPath);
            return null;
        }
        
        // Path to Python executable in virtual environment
        String pythonExecutable = "../python-analysis-service/myenv/bin/python";
        Path pythonPath = Paths.get(pythonExecutable).toAbsolutePath();
        
        if (!Files.exists(pythonPath)) {
            logger.error("Python executable not found at: {}", pythonPath);
            return null;
        }
        
        // Build command
        ProcessBuilder processBuilder = new ProcessBuilder(
            pythonPath.toString(), 
            scriptPath.toString(),
            documentId,
            "--analysis-type", analysisType,
            "--base-dir", "uploads/analysis"
        );
        
        // Set working directory
        processBuilder.directory(new File("."));
        
        // Redirect error stream to output stream
        processBuilder.redirectErrorStream(true);
        
        logger.info("Executing Python script: {}", String.join(" ", processBuilder.command()));
        
        try {
            // Start process
            Process process = processBuilder.start();
            
            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.debug("Python output: {}", line);
                }
            }
            
            // Wait for process to complete (with timeout)
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!finished) {
                logger.error("Python script timed out after 30 seconds");
                process.destroyForcibly();
                return null;
            }
            
            int exitCode = process.exitValue();
            logger.info("Python script finished with exit code: {}", exitCode);
            logger.info("Python script output: {}", output.toString());
            
            if (exitCode == 0) {
                // Extract PDF path from output
                String outputStr = output.toString();
                if (outputStr.contains("SUCCESS:")) {
                    String pdfPath = outputStr.substring(outputStr.indexOf("SUCCESS:") + 8).trim();
                    logger.info("PDF created successfully at: {}", pdfPath);
                    return pdfPath;
                }
            }
            
            logger.error("Python script failed with exit code: {}", exitCode);
            return null;
            
        } catch (Exception e) {
            logger.error("Error executing Python script: {}", e.getMessage(), e);
            return null;
        }
    }
    
    
    /**
     * Check if analysis results are available for merging
     */
    public boolean hasAnalysisResults(Document document, String analysisType) {
        Path analysisDir = Paths.get("uploads/analysis/" + document.getId() + "/" + analysisType);
        if (!Files.exists(analysisDir)) {
            return false;
        }
        
        try {
            return Files.list(analysisDir).findAny().isPresent();
        } catch (IOException e) {
            logger.warn("Error checking analysis results: {}", e.getMessage());
            return false;
        }
    }
}
