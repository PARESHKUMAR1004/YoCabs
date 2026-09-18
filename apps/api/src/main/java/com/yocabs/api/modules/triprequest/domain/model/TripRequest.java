package com.yocabs.api.modules.triprequest.domain.model;

import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.PassengerCount;
import com.yocabs.api.modules.triprequest.domain.valueobject.TravelDateRange;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripBrief;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripRequestId;
import com.yocabs.api.modules.triprequest.domain.valueobject.TouristId;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;

import java.time.Instant;

public class TripRequest {

    private final TripRequestId id;
    private final TouristId touristId;

    private Itinerary itinerary;
    private TravelDateRange travelDateRange;
    private PassengerCount passengerCount;
    private TripBrief tripBrief;

    /*
     * Optional trip-type preference.
     *
     * null means the tourist has not selected
     * a specific trip type and is open to all
     * applicable pricing options.
     */
    private TripType tripType;
    private VehicleCategory vehicleCategory;

    private TripRequestStatus status;
    private long version;

    private final Instant createdAt;
    private Instant updatedAt;

    private TripRequest(
            TripRequestId id,
            TouristId touristId,
            Itinerary itinerary,
            TravelDateRange travelDateRange,
            PassengerCount passengerCount,
            TripBrief tripBrief,
            TripType tripType,
            VehicleCategory vehicleCategory,
            TripRequestStatus status,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.touristId = touristId;
        this.itinerary = itinerary;
        this.travelDateRange = travelDateRange;
        this.passengerCount = passengerCount;
        this.tripBrief = tripBrief;
        this.tripType = tripType;
        this.vehicleCategory = vehicleCategory;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TripRequest create(
            TouristId touristId,
            Itinerary itinerary,
            TravelDateRange travelDateRange,
            PassengerCount passengerCount,
            TripBrief tripBrief,
            TripType tripType,
            VehicleCategory vehicleCategory
    ) {

        Instant now = Instant.now();

        return new TripRequest(
                TripRequestId.generate(),
                touristId,
                itinerary,
                travelDateRange,
                passengerCount,
                tripBrief,
                tripType,
                vehicleCategory,
                TripRequestStatus.DRAFT,
                0L,
                now,
                now
        );
    }

    public static TripRequest reconstitute(
            TripRequestId id,
            TouristId touristId,
            Itinerary itinerary,
            TravelDateRange travelDateRange,
            PassengerCount passengerCount,
            TripBrief tripBrief,
            TripType tripType,
            VehicleCategory vehicleCategory,
            TripRequestStatus status,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new TripRequest(
                id,
                touristId,
                itinerary,
                travelDateRange,
                passengerCount,
                tripBrief,
                tripType,
                vehicleCategory,
                status,
                version,
                createdAt,
                updatedAt
        );
    }

    public void submit() {

        if (status != TripRequestStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only a draft trip request can be submitted"
            );
        }

        status = TripRequestStatus.SUBMITTED;
        touch();
    }

    public void cancel() {

        if (status == TripRequestStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Trip request is already cancelled"
            );
        }

        status = TripRequestStatus.CANCELLED;
        touch();
    }

    public void updateTripBrief(
            TripBrief tripBrief
    ) {

        if (status != TripRequestStatus.DRAFT) {
            throw new IllegalStateException(
                    "Trip brief can only be changed while the trip request is a draft"
            );
        }

        this.tripBrief = tripBrief;
        touch();
    }

    private void touch() {
        version++;
        updatedAt = Instant.now();
    }

    public TripRequestId getId() {
        return id;
    }

    public TouristId getTouristId() {
        return touristId;
    }

    public Itinerary getItinerary() {
        return itinerary;
    }

    public TravelDateRange getTravelDateRange() {
        return travelDateRange;
    }

    public VehicleCategory getVehicleCategory() {
        return vehicleCategory;
    }

    public void setVehicleCategory(VehicleCategory vehicleCategory) {
        this.vehicleCategory = vehicleCategory;
    }

    public PassengerCount getPassengerCount() {
        return passengerCount;
    }

    public TripBrief getTripBrief() {
        return tripBrief;
    }

    public TripType getTripType() {
        return tripType;
    }

    public TripRequestStatus getStatus() {
        return status;
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