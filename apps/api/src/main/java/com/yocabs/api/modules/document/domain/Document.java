package com.yocabs.api.modules.document.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Document {

    public enum OwnerType {
        PARTNER,
        VEHICLE,
        DRIVER
    }

    public enum Type {
        RC_BOOK,
        INSURANCE,
        PUC,
        FITNESS,
        PERMIT,
        DRIVING_LICENSE,
        AADHAAR,
        POLICE_VERIFICATION,
        GST_CERTIFICATE,
        TRADE_LICENSE,
        PROFILE_PHOTO,
        VEHICLE_PHOTO,
        OTHER
    }

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    private final UUID id;
    private final OwnerType ownerType;
    private final UUID ownerId;
    private final Type type;
    private final String storageKey;
    private final String originalFilename;
    private final String contentType;
    private final long sizeBytes;
    private Status status;
    private String rejectionReason;
    private final LocalDate expiryDate;
    private final UUID uploadedBy;
    private UUID reviewedBy;
    private Instant reviewedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    private Document(
            UUID id,
            OwnerType ownerType,
            UUID ownerId,
            Type type,
            String storageKey,
            String originalFilename,
            String contentType,
            long sizeBytes,
            Status status,
            String rejectionReason,
            LocalDate expiryDate,
            UUID uploadedBy,
            UUID reviewedBy,
            Instant reviewedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.type = type;
        this.storageKey = storageKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.expiryDate = expiryDate;
        this.uploadedBy = uploadedBy;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Document upload(
            OwnerType ownerType,
            UUID ownerId,
            Type type,
            String storageKey,
            String originalFilename,
            String contentType,
            long sizeBytes,
            LocalDate expiryDate,
            UUID uploadedBy
    ) {
        if (ownerType == null || ownerId == null || type == null || uploadedBy == null) {
            throw new IllegalArgumentException("Document owner, type and uploader are required");
        }
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("The document is empty");
        }

        Instant now = Instant.now();

        return new Document(
                UUID.randomUUID(), ownerType, ownerId, type, storageKey, originalFilename,
                contentType, sizeBytes, Status.PENDING, null, expiryDate, uploadedBy,
                null, null, now, now
        );
    }

    public static Document reconstitute(
            UUID id,
            OwnerType ownerType,
            UUID ownerId,
            Type type,
            String storageKey,
            String originalFilename,
            String contentType,
            long sizeBytes,
            Status status,
            String rejectionReason,
            LocalDate expiryDate,
            UUID uploadedBy,
            UUID reviewedBy,
            Instant reviewedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Document(
                id, ownerType, ownerId, type, storageKey, originalFilename, contentType,
                sizeBytes, status, rejectionReason, expiryDate, uploadedBy, reviewedBy,
                reviewedAt, createdAt, updatedAt
        );
    }

    public void approve(UUID adminId) {
        requirePending();
        status = Status.APPROVED;
        rejectionReason = null;
        stamp(adminId);
    }

    public void reject(UUID adminId, String reason) {
        requirePending();
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }
        status = Status.REJECTED;
        rejectionReason = reason.trim();
        stamp(adminId);
    }

    private void requirePending() {
        if (status != Status.PENDING) {
            throw new IllegalStateException("This document has already been reviewed");
        }
    }

    private void stamp(UUID adminId) {
        reviewedBy = adminId;
        reviewedAt = Instant.now();
        updatedAt = reviewedAt;
    }

    public UUID getId() { return id; }
    public OwnerType getOwnerType() { return ownerType; }
    public UUID getOwnerId() { return ownerId; }
    public Type getType() { return type; }
    public String getStorageKey() { return storageKey; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public Status getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public UUID getUploadedBy() { return uploadedBy; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
