package com.yocabs.api.modules.standardrate.infrastructure;

import com.yocabs.api.modules.standardrate.domain.StandardRate;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
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
@Table(name = "standard_rates")
public class StandardRateEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private VehicleCategory category;

    @Column(name = "per_km_rate", nullable = false)
    private BigDecimal perKmRate;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StandardRateEntity() {
        // JPA
    }

    static StandardRateEntity fromDomain(StandardRate rate) {
        StandardRateEntity entity = new StandardRateEntity();
        entity.category = rate.category();
        entity.perKmRate = rate.perKmRate();
        entity.updatedBy = rate.updatedBy();
        entity.updatedAt = rate.updatedAt();
        return entity;
    }

    StandardRate toDomain() {
        return new StandardRate(category, perKmRate, updatedBy, updatedAt);
    }
}
