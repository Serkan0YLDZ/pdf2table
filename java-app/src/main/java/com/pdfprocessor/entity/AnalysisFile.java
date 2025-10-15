package com.pdfprocessor.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AnalysisFile entity for storing PDF analysis result files
 */
@Entity
@Table(name = "analysis_files")
public class AnalysisFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @NotNull
    private Document document;

    @Column(name = "analysis_type", nullable = false, length = 50)
    @NotNull
    @Size(max = 50)
    private String analysisType;

    @Column(name = "page_number", nullable = false)
    @NotNull
    @Min(1)
    private Integer pageNumber;

    @Column(name = "result_file_path", nullable = false, length = 500)
    @NotNull
    @Size(max = 500)
    private String resultFilePath;

    @Column(name = "file_size")
    private Long fileSize;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Default constructor
    public AnalysisFile() {
    }

    // Constructor for creating new analysis files
    public AnalysisFile(Document document, String analysisType, Integer pageNumber, String resultFilePath) {
        this.document = document;
        this.analysisType = analysisType;
        this.pageNumber = pageNumber;
        this.resultFilePath = resultFilePath;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getResultFilePath() {
        return resultFilePath;
    }

    public void setResultFilePath(String resultFilePath) {
        this.resultFilePath = resultFilePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AnalysisFile{" +
                "id=" + id +
                ", analysisType='" + analysisType + '\'' +
                ", pageNumber=" + pageNumber +
                ", resultFilePath='" + resultFilePath + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
