package com.pdfprocessor.service;

import com.pdfprocessor.entity.Document;
import com.pdfprocessor.repository.DocumentRepository;
import com.pdfprocessor.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling file operations
 */
@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final String PDF_MIME_TYPE = "application/pdf";

    private final DocumentRepository documentRepository;

    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;

    public FileService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }
    @Transactional
    public Document uploadFile(MultipartFile file) throws IOException, org.springframework.dao.DataAccessException {
        logger.info("Starting file upload: {}", file.getOriginalFilename());

        // Validate file
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE || 
            file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Invalid PDF file");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique file name
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFileName);

        // Save file to disk
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("File saved to: {}", filePath);

        // Create document entity
        Document document = new Document();
        document.setFileName(originalFileName);
        document.setFileSize(file.getSize());
        document.setFilePath(filePath.toString());
        document.setMimeType(file.getContentType());
        document.setUploadDate(LocalDateTime.now());

        // Save to database
        Document savedDocument = documentRepository.save(document);
        logger.info("Document saved to database with ID: {}", savedDocument.getId());
        return savedDocument;
    }

    public List<Document> getAllDocuments() {
        logger.info("Retrieving all documents");
        return documentRepository.findAllByOrderByUploadDateDesc();
    }

    public Optional<Document> getDocumentById(UUID id) {
        logger.info("Retrieving document with ID: {}", id);
        return documentRepository.findById(id);
    }

    public Document getDocumentByFileName(String fileName) {
        logger.info("Retrieving document with filename: {}", fileName);
        return documentRepository.findByFileName(fileName);
    }

    @Transactional
    public boolean deleteDocument(UUID id) {
        logger.info("Deleting document with ID: {}", id);
        
        Optional<Document> documentOpt = documentRepository.findById(id);
        if (documentOpt.isPresent()) {
            Document document = documentOpt.get();
            
            // Delete physical file - sadece I/O işlemi için try-catch
            try {
                Path filePath = Paths.get(document.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    logger.info("Physical file deleted: {}", filePath);
                }
            } catch (IOException e) {
                logger.warn("Could not delete physical file: {}", e.getMessage());
            }

            // Delete from database - I/O işlemi değil, try-catch gereksiz
            documentRepository.deleteById(id);
            logger.info("Document deleted from database: {}", id);
            return true;
        }
        return false;
    }

    public String getFileSizeInHumanReadable(Long fileSize) {
        return FileUtils.getFileSizeInHumanReadable(fileSize);
    }
}
