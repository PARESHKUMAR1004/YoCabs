package com.yocabs.api.modules.settlement.infrastructure;

import com.yocabs.api.modules.settlement.domain.WalletEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_entries")
public class WalletEntryEntity {

    @Id
    private UUID id;

    @Column(name = "travel_partner_id", nullable = false, updatable = false)
    private UUID travelPartnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10, updatable = false)
    private WalletEntry.Type type;

    @Column(name = "amount", nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "reference_type", nullable = false, length = 30, updatable = false)
    private String referenceType;

    @Column(name = "reference_id", nullable = false, updatable = false)
    private UUID referenceId;

    @Column(name = "description", nullable = false, length = 300, updatable = false)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WalletEntryEntity() {
        // JPA
    }

    static WalletEntryEntity fromDomain(WalletEntry entry) {
        WalletEntryEntity entity = new WalletEntryEntity();
        entity.id = entry.id();
        entity.travelPartnerId = entry.travelPartnerId();
        entity.type = entry.type();
        entity.amount = entry.amount();
        entity.referenceType = entry.referenceType();
        entity.referenceId = entry.referenceId();
        entity.description = entry.description();
        entity.createdAt = entry.createdAt();
        return entity;
    }

    WalletEntry toDomain() {
        return new WalletEntry(
                id, travelPartnerId, type, amount, referenceType, referenceId, description, createdAt
        );
    }
}
