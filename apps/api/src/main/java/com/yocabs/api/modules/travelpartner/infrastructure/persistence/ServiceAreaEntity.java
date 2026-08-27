package com.yocabs.api.modules.travelpartner.infrastructure.persistence;

import com.yocabs.api.modules.travelpartner.domain.valueobject.ServiceArea;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "service_areas",
        indexes = {
                @Index(
                        name = "idx_service_areas_travel_partner",
                        columnList = "travel_partner_id"
                )
        }
)
public class ServiceAreaEntity {

    @Id
    private UUID id;

    @Column(
            name = "travel_partner_id",
            nullable = false
    )
    private UUID travelPartnerId;

    @Column(
            name = "name",
            nullable = false,
            length = 200
    )
    private String name;

    @Column(
            name = "latitude",
            nullable = false
    )
    private double latitude;

    @Column(
            name = "longitude",
            nullable = false
    )
    private double longitude;

    @Column(
            name = "radius_km",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private java.math.BigDecimal radiusKm;

    protected ServiceAreaEntity() {
        // JPA
    }

    public ServiceAreaEntity(
            UUID id,
            UUID travelPartnerId,
            String name,
            double latitude,
            double longitude,
            java.math.BigDecimal radiusKm
    ) {
        this.id = id;
        this.travelPartnerId = travelPartnerId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusKm = radiusKm;
    }

    public static ServiceAreaEntity fromDomain(
            UUID travelPartnerId,
            ServiceArea serviceArea
    ) {
        return new ServiceAreaEntity(
                serviceArea.id(),
                travelPartnerId,
                serviceArea.name(),
                serviceArea.latitude(),
                serviceArea.longitude(),
                java.math.BigDecimal.valueOf(
                        serviceArea.radiusKm()
                )
        );
    }

    public ServiceArea toDomain() {
        return new ServiceArea(
                id,
                name,
                latitude,
                longitude,
                radiusKm.doubleValue()
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getTravelPartnerId() {
        return travelPartnerId;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public java.math.BigDecimal getRadiusKm() {
        return radiusKm;
    }
}