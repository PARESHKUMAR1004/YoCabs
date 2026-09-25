package com.yocabs.api.modules.tracking.infrastructure.persistence;

import com.yocabs.api.modules.tracking.domain.model.TripLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "trip_locations",
        indexes = {
                @Index(
                        name = "idx_trip_locations_driver",
                        columnList = "driver_id"
                )
        }
)
public class TripLocationEntity {

    @Id
    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "accuracy_metres")
    private Double accuracyMetres;

    @Column(name = "speed_kph")
    private Double speedKph;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected TripLocationEntity() {
        // JPA
    }

    public static TripLocationEntity fromDomain(
            TripLocation location
    ) {
        TripLocationEntity entity = new TripLocationEntity();
        entity.bookingId = location.bookingId();
        entity.apply(location);
        return entity;
    }

    /** Overwrites the stored position; only the latest point is kept. */
    public void apply(TripLocation location) {
        this.driverId = location.driverId();
        this.latitude = location.latitude();
        this.longitude = location.longitude();
        this.accuracyMetres = location.accuracyMetres();
        this.speedKph = location.speedKph();
        this.recordedAt = location.recordedAt();
    }

    public TripLocation toDomain() {
        return new TripLocation(
                bookingId,
                driverId,
                latitude,
                longitude,
                accuracyMetres,
                speedKph,
                recordedAt
        );
    }
}
