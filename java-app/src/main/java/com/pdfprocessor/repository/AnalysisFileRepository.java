package com.pdfprocessor.repository;

import com.pdfprocessor.entity.AnalysisFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for AnalysisFile entity operations
 */
@Repository
public interface AnalysisFileRepository extends JpaRepository<AnalysisFile, UUID> {

    /**
     * Find all analysis files for a specific document and analysis type
     */
    List<AnalysisFile> findByDocumentIdAndAnalysisType(UUID documentId, String analysisType);

    /**
     * Find all analysis files for a specific document ordered by page number
     */
    List<AnalysisFile> findByDocumentIdOrderByPageNumber(UUID documentId);

    /**
     * Count analysis files for a specific document and analysis type
     */
    long countByDocumentIdAndAnalysisType(UUID documentId, String analysisType);

    /**
     * Delete all analysis files for a specific document (cascade delete)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM AnalysisFile af WHERE af.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);

    /**
     * Find analysis files by document ID and analysis type ordered by page number
     */
    @Query("SELECT af FROM AnalysisFile af WHERE af.document.id = :documentId AND af.analysisType = :analysisType ORDER BY af.pageNumber")
    List<AnalysisFile> findByDocumentIdAndAnalysisTypeOrderByPageNumber(@Param("documentId") UUID documentId, @Param("analysisType") String analysisType);

    /**
     * Check if analysis file exists for specific document, analysis type and page number
     */
    boolean existsByDocumentIdAndAnalysisTypeAndPageNumber(UUID documentId, String analysisType, Integer pageNumber);

    /**
     * Get maximum page number for a specific document and analysis type
     */
    @Query("SELECT MAX(af.pageNumber) FROM AnalysisFile af WHERE af.document.id = :documentId AND af.analysisType = :analysisType")
    Integer findMaxPageNumberByDocumentIdAndAnalysisType(@Param("documentId") UUID documentId, @Param("analysisType") String analysisType);
}
