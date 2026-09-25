package com.yocabs.api.modules.booking.interfaces.rest;

import com.yocabs.api.modules.booking.application.BookingCancellationService;
import com.yocabs.api.modules.booking.application.BookingService;
import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.booking.domain.model.BookingStatus;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import com.yocabs.api.shared.security.Role;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class BookingController {

    private final BookingService bookingService;
    private final BookingCancellationService cancellationService;
    private final BookingViewAssembler assembler;

    public BookingController(
            BookingService bookingService,
            BookingCancellationService cancellationService,
            BookingViewAssembler assembler
    ) {
        this.bookingService = bookingService;
        this.cancellationService = cancellationService;
        this.assembler = assembler;
    }

    @PostMapping("/api/v1/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(
            @CurrentActor Actor actor,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateBookingRequest request
    ) {
        return assembler.toResponse(
                bookingService.createBooking(
                        actor,
                        request.tripRequestId(),
                        request.vehicleId(),
                        request.tripType(),
                        request.negotiationId(),
                        idempotencyKey
                ),
                actor
        );
    }

    @GetMapping("/api/v1/bookings")
    public List<BookingResponse> listMine(@CurrentActor Actor actor) {
        return assembler.toResponses(bookingService.listMine(actor), actor);
    }

    @GetMapping("/api/v1/bookings/{bookingId}")
    public BookingResponse get(
            @CurrentActor Actor actor,
            @PathVariable UUID bookingId
    ) {
        return assembler.toResponse(bookingService.get(actor, bookingId), actor);
    }

    @PostMapping("/api/v1/bookings/{bookingId}/cancel")
    public BookingResponse cancel(
            @CurrentActor Actor actor,
            @PathVariable UUID bookingId,
            @RequestBody(required = false) CancelBookingRequest request
    ) {
        return assembler.toResponse(
                cancellationService.cancel(
                        actor, bookingId, request == null ? null : request.reason()
                ),
                actor
        );
    }

    @GetMapping("/api/v1/travel-partners/{travelPartnerId}/bookings")
    public List<BookingResponse> listForPartner(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @RequestParam(required = false) BookingStatus status
    ) {
        return assembler.toResponses(
                bookingService.listForPartner(actor, travelPartnerId, status), actor
        );
    }

    @GetMapping("/api/v1/driver/bookings")
    public List<BookingResponse> listForDriver(@CurrentActor Actor actor) {
        return assembler.toResponses(bookingService.listForDriver(actor), actor);
    }

    public record CreateBookingRequest(
            UUID tripRequestId,
            UUID vehicleId,
            TripType tripType,
            UUID negotiationId
    ) {
    }

    public record CancelBookingRequest(String reason) {
    }

    public record PriceComponentResponse(String code, String description, BigDecimal amount) {
    }

    public record VehicleSummary(
            UUID id,
            String registrationNumber,
            String make,
            String model,
            String category,
            int passengerCapacity
    ) {
    }

    public record PersonSummary(UUID id, String name, String mobile) {
    }

    public record BookingResponse(
            UUID id,
            UUID tripRequestId,
            UUID travelPartnerId,
            String partnerName,
            UUID vehicleId,
            VehicleSummary vehicle,
            UUID negotiationId,
            TripType tripType,
            LocalDate startDate,
            LocalDate endDate,
            int passengerCount,
            String pickup,
            String destination,
            BookingStatus status,
            String currency,
            BigDecimal totalAmount,
            BigDecimal tokenAmount,
            BigDecimal commissionAmount,
            List<PriceComponentResponse> priceComponents,
            Instant holdExpiresAt,
            UUID driverId,
            PersonSummary driver,
            PersonSummary tourist,
            String cancellationReason,
            Instant createdAt,
            String tripCode
    ) {

        public static BookingResponse from(Booking booking, Actor viewer) {

            // Commission is platform-internal: tourists never see it.
            BigDecimal commission =
                    viewer.role() == Role.TOURIST ? null : booking.getCommissionAmount();

            return new BookingResponse(
                    booking.getId(),
                    booking.getTripRequestId(),
                    booking.getTravelPartnerId(),
                    null,
                    booking.getVehicleId(),
                    null,
                    booking.getNegotiationId(),
                    booking.getTripType(),
                    booking.getStartDate(),
                    booking.getEndDate(),
                    booking.getPassengerCount(),
                    booking.getPickupDescription(),
                    booking.getDestinationDescription(),
                    booking.getStatus(),
                    booking.getCurrency(),
                    booking.getTotalAmount(),
                    booking.getTokenAmount(),
                    commission,
                    booking.getPriceComponents().stream()
                            .map(c -> new PriceComponentResponse(c.code(), c.description(), c.amount()))
                            .toList(),
                    booking.getHoldExpiresAt(),
                    booking.getDriverId(),
                    null,
                    null,
                    booking.getCancellationReason(),
                    booking.getCreatedAt(),
                    // Only the tourist is shown the code; the driver has to be told it in person.
                    viewer.role() == Role.TOURIST ? booking.codeToShowTourist() : null
            );
        }

        BookingResponse withDetails(
                String partnerName,
                VehicleSummary vehicle,
                PersonSummary driver,
                PersonSummary tourist
        ) {
            return new BookingResponse(
                    id, tripRequestId, travelPartnerId, partnerName, vehicleId, vehicle, negotiationId,
                    tripType, startDate, endDate, passengerCount, pickup, destination, status, currency,
                    totalAmount, tokenAmount, commissionAmount, priceComponents, holdExpiresAt,
                    driverId, driver, tourist, cancellationReason, createdAt, tripCode
            );
        }
    }
}
