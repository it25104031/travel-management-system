package com.travel.module2_visa.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * An uploaded file for a visa application. Each destination keeps its documents in its own table
 * (india_visa_application_documents, srilanka_visa_application_documents) so the foreign key points at
 * that destination's application table; ids are shared across them, like application ids.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class ApplicationDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "visa_document_seq")
    @SequenceGenerator(name = "visa_document_seq", sequenceName = "visa_document_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String fileName;
    private String fileType;
    @Column(nullable = false, length = 500)
    private String filePath;
    private Long fileSize;

    // Which required-document slot this file fills (PHOTO, PASSPORT_BIO, ...); OTHER for extra uploads
    @Column(name = "document_type")
    private String docCode;

    // Verification workflow
    @Column(name = "verification_status")
    private String status; // DocumentStatus
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;
    private LocalDate expiryDate;
    private String reviewedBy;
    private LocalDateTime reviewedAt;

    // Version history: a replacement points at the document it superseded
    @Column(name = "version_number")
    private Integer version = 1;
    private Long previousVersionId;
    private String uploadedBy; // staff username or customer reference number


    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
        if (this.status == null) this.status = DocumentStatus.UPLOADED.name();
        if (this.version == null) this.version = 1;
    }

    public DocumentStatus getStatusEnum() {
        if (status == null) return DocumentStatus.UPLOADED;
        try { return DocumentStatus.valueOf(status); } catch (IllegalArgumentException e) { return DocumentStatus.UPLOADED; }
    }

    public boolean isImage() { return fileType != null && fileType.startsWith("image"); }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getDocCode() { return docCode; }
    public void setDocCode(String docCode) { this.docCode = docCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Long getPreviousVersionId() { return previousVersionId; }
    public void setPreviousVersionId(Long previousVersionId) { this.previousVersionId = previousVersionId; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public abstract VisaApplicationEntity getVisaApplication();

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
