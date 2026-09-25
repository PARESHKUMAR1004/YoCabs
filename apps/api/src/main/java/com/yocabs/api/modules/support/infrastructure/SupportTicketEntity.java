package com.yocabs.api.modules.support.infrastructure;

import com.yocabs.api.modules.support.domain.SupportTicket;
import com.yocabs.api.shared.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_tickets")
public class SupportTicketEntity {

    @Id
    private UUID id;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "creator_role", nullable = false, length = 30, updatable = false)
    private Role creatorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30, updatable = false)
    private SupportTicket.Category category;

    @Column(name = "subject", nullable = false, length = 200, updatable = false)
    private String subject;

    @Column(name = "booking_id", updatable = false)
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SupportTicket.Status status;

    @Column(name = "assigned_admin_id")
    private UUID assignedAdminId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected SupportTicketEntity() {
        // JPA
    }

    static SupportTicketEntity fromDomain(SupportTicket ticket) {
        SupportTicketEntity entity = new SupportTicketEntity();
        entity.id = ticket.getId();
        entity.createdBy = ticket.getCreatedBy();
        entity.creatorRole = ticket.getCreatorRole();
        entity.category = ticket.getCategory();
        entity.subject = ticket.getSubject();
        entity.bookingId = ticket.getBookingId();
        entity.createdAt = ticket.getCreatedAt();
        entity.apply(ticket);
        return entity;
    }

    void apply(SupportTicket ticket) {
        this.status = ticket.getStatus();
        this.assignedAdminId = ticket.getAssignedAdminId();
        this.updatedAt = ticket.getUpdatedAt();
        this.resolvedAt = ticket.getResolvedAt();
    }

    SupportTicket toDomain() {
        return SupportTicket.reconstitute(
                id, createdBy, creatorRole, category, subject, bookingId, status,
                assignedAdminId, version, createdAt, updatedAt, resolvedAt
        );
    }
}
