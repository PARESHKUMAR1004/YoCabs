package com.yocabs.api.modules.popularplace.infrastructure;

import com.yocabs.api.modules.popularplace.domain.PopularPlace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "popular_places")
public class PopularPlaceEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String subtitle;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PopularPlaceEntity() {
        // JPA
    }

    static PopularPlaceEntity fromDomain(PopularPlace place) {
        PopularPlaceEntity entity = new PopularPlaceEntity();
        entity.id = place.id();
        entity.name = place.name();
        entity.subtitle = place.subtitle();
        entity.latitude = place.latitude();
        entity.longitude = place.longitude();
        entity.displayOrder = place.displayOrder();
        entity.updatedBy = place.updatedBy();
        entity.updatedAt = place.updatedAt();
        return entity;
    }

    PopularPlace toDomain() {
        return new PopularPlace(id, name, subtitle, latitude, longitude, displayOrder, updatedBy, updatedAt);
    }
}
