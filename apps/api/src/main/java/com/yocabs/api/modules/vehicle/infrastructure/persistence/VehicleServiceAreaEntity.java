package com.yocabs.api.modules.vehicle.infrastructure.persistence;

import com.yocabs.api.modules.vehicle.domain.valueobject.ServiceArea;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "vehicle_service_areas",
        indexes = {
                @Index(
                        name = "idx_vehicle_service_areas_vehicle",
                        columnList = "vehicle_id"
                )
        }
)
public class VehicleServiceAreaEntity {

    @Id
    private UUID id;

    @Column(
            name = "vehicle_id",
            nullable = false
    )
    private UUID vehicleId;

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
    private BigDecimal radiusKm;

    protected VehicleServiceAreaEntity() {
        // JPA
    }

    public VehicleServiceAreaEntity(
            UUID id,
            UUID vehicleId,
            String name,
            double latitude,
            double longitude,
            BigDecimal radiusKm
    ) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusKm = radiusKm;
    }

    public static VehicleServiceAreaEntity fromDomain(
            UUID vehicleId,
            ServiceArea serviceArea
    ) {
        return new VehicleServiceAreaEntity(
                serviceArea.id(),
                vehicleId,
                serviceArea.name(),
                serviceArea.latitude(),
                serviceArea.longitude(),
                BigDecimal.valueOf(serviceArea.radiusKm())
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

    public UUID getVehicleId() {
        return vehicleId;
    }
}
