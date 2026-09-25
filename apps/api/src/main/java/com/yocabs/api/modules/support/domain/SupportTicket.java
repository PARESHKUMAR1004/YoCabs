package com.yocabs.api.modules.support.domain;

import com.yocabs.api.shared.security.Role;

import java.time.Instant;
import java.util.UUID;

public class SupportTicket {

    public enum Category {
        BOOKING_ISSUE,
        PAYMENT_ISSUE,
        DRIVER_ISSUE,
        CANCELLATION_REFUND,
        SAFETY,
        OTHER
    }

    public enum Status {
        OPEN,
        IN_PROGRESS,
        RESOLVED,
        CLOSED
    }

    private final UUID id;
    private final UUID createdBy;
    private final Role creatorRole;
    private final Category category;
    private final String subject;
    private final UUID bookingId;
    private Status status;
    private UUID assignedAdminId;
    private final long version;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;

    private SupportTicket(
            UUID id,
            UUID createdBy,
            Role creatorRole,
            Category category,
            String subject,
            UUID bookingId,
            Status status,
            UUID assignedAdminId,
            long version,
            Instant createdAt,
            Instant updatedAt,
            Instant resolvedAt
    ) {
        this.id = id;
        this.createdBy = createdBy;
        this.creatorRole = creatorRole;
        this.category = category;
        this.subject = subject;
        this.bookingId = bookingId;
        this.status = status;
        this.assignedAdminId = assignedAdminId;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.resolvedAt = resolvedAt;
    }

    public static SupportTicket open(
            UUID createdBy,
            Role creatorRole,
            Category category,
            String subject,
            UUID bookingId
    ) {
        if (createdBy == null || creatorRole == null) {
            throw new IllegalArgumentException("A ticket needs a creator");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category is required");
        }
        if (subject == null || subject.isBlank() || subject.length() > 200) {
            throw new IllegalArgumentException("Subject is required (max 200 characters)");
        }

        Instant now = Instant.now();

        return new SupportTicket(
                UUID.randomUUID(), createdBy, creatorRole, category, subject.trim(),
                bookingId, Status.OPEN, null, 0L, now, now, null
        );
    }

    public static SupportTicket reconstitute(
            UUID id,
            UUID createdBy,
            Role creatorRole,
            Category category,
            String subject,
            UUID bookingId,
            Status status,
            UUID assignedAdminId,
            long version,
            Instant createdAt,
            Instant updatedAt,
            Instant resolvedAt
    ) {
        return new SupportTicket(
                id, createdBy, creatorRole, category, subject, bookingId, status,
                assignedAdminId, version, createdAt, updatedAt, resolvedAt
        );
    }

    public boolean acceptsMessages() {
        return status != Status.CLOSED;
    }

    public void assignTo(UUID adminId) {
        requireNotClosed();
        assignedAdminId = adminId;
        if (status == Status.OPEN) {
            status = Status.IN_PROGRESS;
        }
        touch();
    }

    public void resolve() {
        if (status != Status.OPEN && status != Status.IN_PROGRESS) {
            throw new IllegalStateException("Only an open ticket can be resolved");
        }
        status = Status.RESOLVED;
        resolvedAt = Instant.now();
        touch();
    }

    public void close() {
        requireNotClosed();
        status = Status.CLOSED;
        touch();
    }

    /** The creator replied after resolution: the issue is not settled after all. */
    public void reopen() {
        if (status != Status.RESOLVED) {
            throw new IllegalStateException("Only a resolved ticket can be reopened");
        }
        status = assignedAdminId == null ? Status.OPEN : Status.IN_PROGRESS;
        resolvedAt = null;
        touch();
    }

    public void touch() {
        updatedAt = Instant.now();
    }

    private void requireNotClosed() {
        if (status == Status.CLOSED) {
            throw new IllegalStateException("This ticket is closed");
        }
    }

    public UUID getId() { return id; }
    public UUID getCreatedBy() { return createdBy; }
    public Role getCreatorRole() { return creatorRole; }
    public Category getCategory() { return category; }
    public String getSubject() { return subject; }
    public UUID getBookingId() { return bookingId; }
    public Status getStatus() { return status; }
    public UUID getAssignedAdminId() { return assignedAdminId; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}
