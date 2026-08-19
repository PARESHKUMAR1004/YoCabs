package com.yocabs.api.modules.triprequest.infrastructure.persistence;

import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "trip_request_stops")
public class TripRequestStopEntity {

    @Id
    private UUID id;

    @Column(name = "trip_request_id", nullable = false)
    private UUID tripRequestId;

    @Column(name = "stop_order", nullable = false)
    private int stopOrder;

    @Column(nullable = false, length = 500)
    private String description;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    protected TripRequestStopEntity() {
        // Required by JPA
    }

    public static TripRequestStopEntity from(
            UUID tripRequestId,
            int stopOrder,
            Location location
    ) {
        TripRequestStopEntity entity =
                new TripRequestStopEntity();

        entity.id = UUID.randomUUID();
        entity.tripRequestId = tripRequestId;
        entity.stopOrder = stopOrder;
        entity.description = location.description();
        entity.latitude = location.latitude();
        entity.longitude = location.longitude();

        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTripRequestId() {
        return tripRequestId;
    }

    public int getStopOrder() {
        return stopOrder;
    }

    public String getDescription() {
        return description;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}