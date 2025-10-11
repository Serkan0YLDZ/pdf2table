package com.pdfprocessor.repository;

import com.pdfprocessor.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Document entity operations
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Find all documents ordered by upload date (newest first)
     */
    List<Document> findAllByOrderByUploadDateDesc();

    /**
     * Find document by filename
     */
    Document findByFileName(String fileName);
}
