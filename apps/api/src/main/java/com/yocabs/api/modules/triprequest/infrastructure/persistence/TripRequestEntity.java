package com.yocabs.api.modules.triprequest.infrastructure.persistence;

import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "trip_requests")
public class TripRequestEntity {

    @Id
    private UUID id;

    @Column(name = "tourist_id", nullable = false)
    private UUID touristId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "passenger_count", nullable = false)
    private int passengerCount;

    @Column(name = "trip_brief", nullable = false)
    private String tripBrief;

    @Column(name = "pickup_description", nullable = false, length = 500)
    private String pickupDescription;

    @Column(name = "pickup_latitude")
    private Double pickupLatitude;

    @Column(name = "pickup_longitude")
    private Double pickupLongitude;

    @Column(name = "destination_description", nullable = false, length = 500)
    private String destinationDescription;

    @Column(name = "destination_latitude")
    private Double destinationLatitude;

    @Column(name = "destination_longitude")
    private Double destinationLongitude;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TripRequestEntity() {
        // Required by JPA
    }

    public static TripRequestEntity from(TripRequest tripRequest) {
        TripRequestEntity entity = new TripRequestEntity();

        entity.id = tripRequest.getId().value();
        entity.touristId = tripRequest.getTouristId().value();
        entity.status = tripRequest.getStatus().name();

        entity.startDate =
                tripRequest.getTravelDateRange().startDate();

        entity.endDate =
                tripRequest.getTravelDateRange().endDate();

        entity.passengerCount =
                tripRequest.getPassengerCount().value();

        entity.tripBrief =
                tripRequest.getTripBrief().value();

        Location pickup = tripRequest.getItinerary().pickup();

        entity.pickupDescription = pickup.description();
        entity.pickupLatitude = pickup.latitude();
        entity.pickupLongitude = pickup.longitude();

        Location destination =
                tripRequest.getItinerary().destination();

        entity.destinationDescription = destination.description();
        entity.destinationLatitude = destination.latitude();
        entity.destinationLongitude = destination.longitude();

        entity.version = tripRequest.getVersion();
        entity.createdAt = tripRequest.getCreatedAt();
        entity.updatedAt = tripRequest.getUpdatedAt();

        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTouristId() {
        return touristId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public String getTripBrief() {
        return tripBrief;
    }

    public String getPickupDescription() {
        return pickupDescription;
    }

    public Double getPickupLatitude() {
        return pickupLatitude;
    }

    public Double getPickupLongitude() {
        return pickupLongitude;
    }

    public String getDestinationDescription() {
        return destinationDescription;
    }

    public Double getDestinationLatitude() {
        return destinationLatitude;
    }

    public Double getDestinationLongitude() {
        return destinationLongitude;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}