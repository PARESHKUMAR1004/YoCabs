package com.yocabs.api.modules.vehicle.infrastructure.persistence;

import com.yocabs.api.modules.vehicle.domain.model.Facility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "facilities")
public class FacilityEntity {

    @Id
    @Column(name = "code", length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FacilityEntity() {
        // JPA
    }

    static FacilityEntity fromDomain(Facility facility, Instant createdAt) {
        FacilityEntity entity = new FacilityEntity();
        entity.code = facility.code();
        entity.name = facility.name();
        entity.active = facility.active();
        entity.createdAt = createdAt;
        return entity;
    }

    void apply(Facility facility) {
        this.name = facility.name();
        this.active = facility.active();
    }

    Facility toDomain() {
        return new Facility(code, name, active);
    }
}
