package com.yocabs.api.modules.document.infrastructure;

import com.yocabs.api.modules.document.domain.Document;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private Document.OwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private Document.Type type;

    @Column(name = "storage_key", nullable = false, length = 200)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Document.Status status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DocumentEntity() {
        // JPA
    }

    static DocumentEntity fromDomain(Document document) {
        DocumentEntity entity = new DocumentEntity();
        entity.id = document.getId();
        entity.ownerType = document.getOwnerType();
        entity.ownerId = document.getOwnerId();
        entity.type = document.getType();
        entity.storageKey = document.getStorageKey();
        entity.originalFilename = document.getOriginalFilename();
        entity.contentType = document.getContentType();
        entity.sizeBytes = document.getSizeBytes();
        entity.expiryDate = document.getExpiryDate();
        entity.uploadedBy = document.getUploadedBy();
        entity.createdAt = document.getCreatedAt();
        entity.updateFromDomain(document);
        return entity;
    }

    void updateFromDomain(Document document) {
        this.status = document.getStatus();
        this.rejectionReason = document.getRejectionReason();
        this.reviewedBy = document.getReviewedBy();
        this.reviewedAt = document.getReviewedAt();
        this.updatedAt = document.getUpdatedAt();
    }

    Document toDomain() {
        return Document.reconstitute(
                id, ownerType, ownerId, type, storageKey, originalFilename, contentType,
                sizeBytes, status, rejectionReason, expiryDate, uploadedBy, reviewedBy,
                reviewedAt, createdAt, updatedAt
        );
    }
}
