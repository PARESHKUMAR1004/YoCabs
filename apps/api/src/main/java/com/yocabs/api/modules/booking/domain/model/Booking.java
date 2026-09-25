package com.yocabs.api.modules.booking.domain.model;

import com.yocabs.api.modules.pricing.domain.PriceComponent;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.shared.security.Role;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class Booking {

    private static final SecureRandom CODE_RANDOM = new SecureRandom();

    private final UUID id;
    private final UUID tripRequestId;
    private final UUID touristId;
    private final UUID travelPartnerId;
    private final UUID vehicleId;
    private final UUID negotiationId;
    private final TripType tripType;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int passengerCount;
    private final String pickupDescription;
    private final String destinationDescription;
    private BookingStatus status;
    private final String currency;
    private final BigDecimal totalAmount;
    private final BigDecimal tokenAmount;
    private final BigDecimal commissionAmount;
    private final List<PriceComponent> priceComponents;
    private Instant holdExpiresAt;
    private UUID driverId;
    private final String idempotencyKey;
    private final String startCode;
    private final String completionCode;
    private String cancellationReason;
    private Role cancelledByRole;
    private final long version;
    private final Instant createdAt;
    private Instant updatedAt;

    private Booking(
            UUID id,
            UUID tripRequestId,
            UUID touristId,
            UUID travelPartnerId,
            UUID vehicleId,
            UUID negotiationId,
            TripType tripType,
            LocalDate startDate,
            LocalDate endDate,
            int passengerCount,
            String pickupDescription,
            String destinationDescription,
            BookingStatus status,
            String currency,
            BigDecimal totalAmount,
            BigDecimal tokenAmount,
            BigDecimal commissionAmount,
            List<PriceComponent> priceComponents,
            Instant holdExpiresAt,
            UUID driverId,
            String idempotencyKey,
            String startCode,
            String completionCode,
            String cancellationReason,
            Role cancelledByRole,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.tripRequestId = tripRequestId;
        this.touristId = touristId;
        this.travelPartnerId = travelPartnerId;
        this.vehicleId = vehicleId;
        this.negotiationId = negotiationId;
        this.tripType = tripType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.passengerCount = passengerCount;
        this.pickupDescription = pickupDescription;
        this.destinationDescription = destinationDescription;
        this.status = status;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.tokenAmount = tokenAmount;
        this.commissionAmount = commissionAmount;
        this.priceComponents = List.copyOf(priceComponents);
        this.holdExpiresAt = holdExpiresAt;
        this.driverId = driverId;
        this.idempotencyKey = idempotencyKey;
        this.startCode = startCode;
        this.completionCode = completionCode;
        this.cancellationReason = cancellationReason;
        this.cancelledByRole = cancelledByRole;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Booking create(
            UUID tripRequestId,
            UUID touristId,
            UUID travelPartnerId,
            UUID vehicleId,
            UUID negotiationId,
            TripType tripType,
            LocalDate startDate,
            LocalDate endDate,
            int passengerCount,
            String pickupDescription,
            String destinationDescription,
            String currency,
            BigDecimal totalAmount,
            BigDecimal tokenAmount,
            BigDecimal commissionAmount,
            List<PriceComponent> priceComponents,
            Instant holdExpiresAt,
            String idempotencyKey
    ) {
        if (tripRequestId == null || touristId == null || travelPartnerId == null
                || vehicleId == null || tripType == null) {
            throw new IllegalArgumentException("Booking is missing required references");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException("A valid idempotency key is required");
        }

        if (totalAmount == null || totalAmount.signum() < 0
                || tokenAmount == null || tokenAmount.signum() < 0
                || commissionAmount == null || commissionAmount.signum() < 0) {
            throw new IllegalArgumentException("Booking amounts cannot be negative");
        }

        Instant now = Instant.now();

        return new Booking(
                UUID.randomUUID(), tripRequestId, touristId, travelPartnerId, vehicleId,
                negotiationId, tripType, startDate, endDate, passengerCount,
                pickupDescription, destinationDescription, BookingStatus.PENDING_PAYMENT,
                currency, totalAmount, tokenAmount, commissionAmount, priceComponents,
                holdExpiresAt, null, idempotencyKey, newTripCode(), newTripCode(), null, null, 0L, now, now
        );
    }

    public static Booking reconstitute(
            UUID id,
            UUID tripRequestId,
            UUID touristId,
            UUID travelPartnerId,
            UUID vehicleId,
            UUID negotiationId,
            TripType tripType,
            LocalDate startDate,
            LocalDate endDate,
            int passengerCount,
            String pickupDescription,
            String destinationDescription,
            BookingStatus status,
            String currency,
            BigDecimal totalAmount,
            BigDecimal tokenAmount,
            BigDecimal commissionAmount,
            List<PriceComponent> priceComponents,
            Instant holdExpiresAt,
            UUID driverId,
            String idempotencyKey,
            String startCode,
            String completionCode,
            String cancellationReason,
            Role cancelledByRole,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Booking(
                id, tripRequestId, touristId, travelPartnerId, vehicleId, negotiationId, tripType,
                startDate, endDate, passengerCount, pickupDescription, destinationDescription,
                status, currency, totalAmount, tokenAmount, commissionAmount, priceComponents,
                holdExpiresAt, driverId, idempotencyKey, startCode, completionCode, cancellationReason,
                cancelledByRole, version, createdAt, updatedAt
        );
    }

    public boolean isHoldExpired(Instant now) {
        return status == BookingStatus.PENDING_PAYMENT
                && holdExpiresAt != null
                && !holdExpiresAt.isAfter(now);
    }

    /** Whether this booking currently reserves its vehicle for its dates. */
    public boolean blocksVehicle(Instant now) {
        return switch (status) {
            case CONFIRMED, IN_PROGRESS -> true;
            case PENDING_PAYMENT -> !isHoldExpired(now);
            default -> false;
        };
    }

    public void confirm(Instant now) {
        if (status != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException(
                    "Only a booking awaiting payment can be confirmed"
            );
        }
        status = BookingStatus.CONFIRMED;
        holdExpiresAt = null;
        updatedAt = now;
    }

    /** Payment arrived after the hold lapsed but the vehicle is still free. */
    public void reviveAndConfirm(Instant now) {
        if (status != BookingStatus.EXPIRED) {
            throw new IllegalStateException("Only an expired booking can be revived");
        }
        status = BookingStatus.CONFIRMED;
        holdExpiresAt = null;
        updatedAt = now;
    }

    public void expire(Instant now) {
        if (!isHoldExpired(now)) {
            throw new IllegalStateException("The booking hold has not expired");
        }
        status = BookingStatus.EXPIRED;
        updatedAt = now;
    }

    public void cancel(String reason, Role cancelledBy, Instant now) {
        if (status != BookingStatus.PENDING_PAYMENT && status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "A booking can only be cancelled before the trip starts"
            );
        }
        status = BookingStatus.CANCELLED;
        cancellationReason = reason == null || reason.isBlank() ? null : reason.trim();
        cancelledByRole = cancelledBy;
        updatedAt = now;
    }

    public void assignDriver(UUID newDriverId, Instant now) {
        if (newDriverId == null) {
            throw new IllegalArgumentException("Driver is required");
        }
        if (status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "A driver can only be assigned to a confirmed booking"
            );
        }
        driverId = newDriverId;
        updatedAt = now;
    }

    public void startTrip(LocalDate today, String code, Instant now) {
        if (status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only a confirmed booking can be started");
        }
        if (driverId == null) {
            throw new IllegalStateException("Assign a driver before starting the trip");
        }
        if (today.isBefore(startDate)) {
            throw new IllegalStateException("The trip cannot start before its travel date");
        }
        requireCode(startCode, code);
        status = BookingStatus.IN_PROGRESS;
        updatedAt = now;
    }

    public void completeTrip(String code, Instant now) {
        if (status != BookingStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only a trip in progress can be completed");
        }
        requireCode(completionCode, code);
        status = BookingStatus.COMPLETED;
        updatedAt = now;
    }

    /**
     * The code the tourist reads out at the moment the trip needs proof that they are present:
     * the start code before the trip begins, the completion code while it runs, nothing otherwise.
     */
    public String codeToShowTourist() {
        if (status == BookingStatus.CONFIRMED && driverId != null) {
            return startCode;
        }
        return status == BookingStatus.IN_PROGRESS ? completionCode : null;
    }

    public String getStartCode() { return startCode; }
    public String getCompletionCode() { return completionCode; }

    private static void requireCode(String expected, String supplied) {
        if (supplied == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.trim().getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("The trip code is not correct");
        }
    }

    private static String newTripCode() {
        return String.format("%06d", CODE_RANDOM.nextInt(1_000_000));
    }

    public UUID getId() { return id; }
    public UUID getTripRequestId() { return tripRequestId; }
    public UUID getTouristId() { return touristId; }
    public UUID getTravelPartnerId() { return travelPartnerId; }
    public UUID getVehicleId() { return vehicleId; }
    public UUID getNegotiationId() { return negotiationId; }
    public TripType getTripType() { return tripType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getPassengerCount() { return passengerCount; }
    public String getPickupDescription() { return pickupDescription; }
    public String getDestinationDescription() { return destinationDescription; }
    public BookingStatus getStatus() { return status; }
    public String getCurrency() { return currency; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getTokenAmount() { return tokenAmount; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public List<PriceComponent> getPriceComponents() { return priceComponents; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; }
    public UUID getDriverId() { return driverId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getCancellationReason() { return cancellationReason; }
    public Role getCancelledByRole() { return cancelledByRole; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
