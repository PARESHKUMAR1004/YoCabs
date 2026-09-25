package com.yocabs.api.modules.travelpartner.infrastructure.persistence;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartnerStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Entity
@Table(
        name = "travel_partners",
        indexes = {
                @Index(
                        name = "idx_travel_partners_status",
                        columnList = "status"
                )
        }
)
public class TravelPartnerEntity {

    @Id
    private UUID id;

    @Column(
            name = "name",
            nullable = false,
            length = 200
    )
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private TravelPartnerStatus status;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    protected TravelPartnerEntity() {
        // JPA
    }

    public TravelPartnerEntity(
            UUID id,
            String name,
            TravelPartnerStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TravelPartnerEntity fromDomain(
            TravelPartner travelPartner
    ) {
        return new TravelPartnerEntity(
                travelPartner.getId(),
                travelPartner.getName(),
                travelPartner.getStatus(),
                travelPartner.getCreatedAt(),
                travelPartner.getUpdatedAt()
        );
    }

    public TravelPartner toDomain() {
        return TravelPartner.reconstitute(
                id,
                name,
                status,
                createdAt,
                updatedAt
        );
    }

    public void updateFromDomain(
            TravelPartner travelPartner
    ) {
        this.name = travelPartner.getName();
        this.status = travelPartner.getStatus();
        this.updatedAt = travelPartner.getUpdatedAt();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TravelPartnerStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}