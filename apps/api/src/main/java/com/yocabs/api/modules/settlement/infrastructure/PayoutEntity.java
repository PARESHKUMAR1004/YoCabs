package com.yocabs.api.modules.settlement.infrastructure;

import com.yocabs.api.modules.settlement.domain.Payout;
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
@Table(name = "payouts")
public class PayoutEntity {

    @Id
    private UUID id;

    @Column(name = "travel_partner_id", nullable = false)
    private UUID travelPartnerId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Payout.Status status;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "bank_reference", length = 100)
    private String bankReference;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected PayoutEntity() {
        // JPA
    }

    static PayoutEntity fromDomain(Payout payout) {
        PayoutEntity entity = new PayoutEntity();
        entity.id = payout.getId();
        entity.travelPartnerId = payout.getTravelPartnerId();
        entity.amount = payout.getAmount();
        entity.requestedBy = payout.getRequestedBy();
        entity.createdAt = payout.getCreatedAt();
        entity.updateFromDomain(payout);
        return entity;
    }

    void updateFromDomain(Payout payout) {
        this.status = payout.getStatus();
        this.processedBy = payout.getProcessedBy();
        this.bankReference = payout.getBankReference();
        this.note = payout.getNote();
        this.processedAt = payout.getProcessedAt();
    }

    Payout toDomain() {
        return Payout.reconstitute(
                id, travelPartnerId, amount, status, requestedBy, processedBy,
                bankReference, note, createdAt, processedAt
        );
    }
}
