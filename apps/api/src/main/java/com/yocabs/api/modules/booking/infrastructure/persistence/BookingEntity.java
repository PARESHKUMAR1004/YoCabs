package com.yocabs.api.modules.booking.infrastructure.persistence;

import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.booking.domain.model.BookingStatus;
import com.yocabs.api.modules.pricing.domain.PriceComponent;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.shared.security.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class BookingEntity {

    @Id
    private UUID id;

    @Column(name = "trip_request_id", nullable = false)
    private UUID tripRequestId;

    @Column(name = "tourist_id", nullable = false)
    private UUID touristId;

    @Column(name = "travel_partner_id", nullable = false)
    private UUID travelPartnerId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "negotiation_id")
    private UUID negotiationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_type", nullable = false, length = 50)
    private TripType tripType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "passenger_count", nullable = false)
    private int passengerCount;

    @Column(name = "pickup_description", nullable = false, length = 500)
    private String pickupDescription;

    @Column(name = "destination_description", nullable = false, length = 500)
    private String destinationDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BookingStatus status;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "token_amount", nullable = false)
    private BigDecimal tokenAmount;

    @Column(name = "commission_amount", nullable = false)
    private BigDecimal commissionAmount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "booking_price_components",
            joinColumns = @JoinColumn(name = "booking_id")
    )
    @OrderColumn(name = "position")
    private List<PriceComponentEmbeddable> priceComponents = new ArrayList<>();

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by_role", length = 30)
    private Role cancelledByRole;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BookingEntity() {
        // JPA
    }

    static BookingEntity fromDomain(Booking booking) {
        BookingEntity entity = new BookingEntity();
        entity.id = booking.getId();
        entity.tripRequestId = booking.getTripRequestId();
        entity.touristId = booking.getTouristId();
        entity.travelPartnerId = booking.getTravelPartnerId();
        entity.vehicleId = booking.getVehicleId();
        entity.negotiationId = booking.getNegotiationId();
        entity.tripType = booking.getTripType();
        entity.startDate = booking.getStartDate();
        entity.endDate = booking.getEndDate();
        entity.passengerCount = booking.getPassengerCount();
        entity.pickupDescription = booking.getPickupDescription();
        entity.destinationDescription = booking.getDestinationDescription();
        entity.currency = booking.getCurrency();
        entity.totalAmount = booking.getTotalAmount();
        entity.tokenAmount = booking.getTokenAmount();
        entity.commissionAmount = booking.getCommissionAmount();
        entity.idempotencyKey = booking.getIdempotencyKey();
        entity.createdAt = booking.getCreatedAt();

        for (PriceComponent component : booking.getPriceComponents()) {
            entity.priceComponents.add(
                    new PriceComponentEmbeddable(
                            component.code(), component.description(), component.amount()
                    )
            );
        }

        entity.updateFromDomain(booking);
        return entity;
    }

    void updateFromDomain(Booking booking) {
        this.status = booking.getStatus();
        this.holdExpiresAt = booking.getHoldExpiresAt();
        this.driverId = booking.getDriverId();
        this.cancellationReason = booking.getCancellationReason();
        this.cancelledByRole = booking.getCancelledByRole();
        this.updatedAt = booking.getUpdatedAt();
    }

    Booking toDomain() {
        return Booking.reconstitute(
                id, tripRequestId, touristId, travelPartnerId, vehicleId, negotiationId, tripType,
                startDate, endDate, passengerCount, pickupDescription, destinationDescription,
                status, currency, totalAmount, tokenAmount, commissionAmount,
                priceComponents.stream()
                        .map(c -> new PriceComponent(c.getCode(), c.getDescription(), c.getAmount()))
                        .toList(),
                holdExpiresAt, driverId, idempotencyKey, cancellationReason, cancelledByRole,
                version, createdAt, updatedAt
        );
    }
}
