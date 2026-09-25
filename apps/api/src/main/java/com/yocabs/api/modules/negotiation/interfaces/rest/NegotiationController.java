package com.yocabs.api.modules.negotiation.interfaces.rest;

import com.yocabs.api.modules.negotiation.application.NegotiationService;
import com.yocabs.api.modules.negotiation.application.NegotiationService.PartnerDecision;
import com.yocabs.api.modules.negotiation.domain.model.Negotiation;
import com.yocabs.api.modules.negotiation.domain.model.NegotiationStatus;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class NegotiationController {

    private final NegotiationService negotiationService;
    private final NegotiationViewAssembler assembler;

    public NegotiationController(
            NegotiationService negotiationService,
            NegotiationViewAssembler assembler
    ) {
        this.negotiationService = negotiationService;
        this.assembler = assembler;
    }

    @PostMapping("/api/v1/trip-requests/{tripRequestId}/negotiations")
    @ResponseStatus(HttpStatus.CREATED)
    public NegotiationResponse start(
            @CurrentActor Actor actor,
            @PathVariable UUID tripRequestId,
            @RequestBody StartNegotiationRequest request
    ) {
        return assembler.toResponse(
                negotiationService.startNegotiation(
                        actor, tripRequestId, request.vehicleId(),
                        request.tripType(), request.offeredAmount()
                )
        );
    }

    @GetMapping("/api/v1/trip-requests/{tripRequestId}/negotiations")
    public List<NegotiationResponse> listForTripRequest(
            @CurrentActor Actor actor,
            @PathVariable UUID tripRequestId
    ) {
        return assembler.toResponses(negotiationService.listForTripRequest(actor, tripRequestId));
    }

    @GetMapping("/api/v1/negotiations/{negotiationId}")
    public NegotiationResponse get(
            @CurrentActor Actor actor,
            @PathVariable UUID negotiationId
    ) {
        return assembler.toResponse(negotiationService.get(actor, negotiationId));
    }

    @PostMapping("/api/v1/negotiations/{negotiationId}/respond")
    public NegotiationResponse respond(
            @CurrentActor Actor actor,
            @PathVariable UUID negotiationId,
            @RequestBody PartnerResponseRequest request
    ) {
        return assembler.toResponse(
                negotiationService.partnerRespond(
                        actor, negotiationId, request.decision(), request.counterAmount()
                )
        );
    }

    @PostMapping("/api/v1/negotiations/{negotiationId}/counter-response")
    public NegotiationResponse respondToCounter(
            @CurrentActor Actor actor,
            @PathVariable UUID negotiationId,
            @RequestBody CounterResponseRequest request
    ) {
        return assembler.toResponse(
                negotiationService.touristRespondToCounter(
                        actor, negotiationId, Boolean.TRUE.equals(request.accept())
                )
        );
    }

    @GetMapping("/api/v1/travel-partners/{travelPartnerId}/negotiations")
    public List<NegotiationResponse> listForPartner(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @RequestParam(required = false) NegotiationStatus status
    ) {
        return assembler.toResponses(negotiationService.listForPartner(actor, travelPartnerId, status));
    }

    public record StartNegotiationRequest(
            UUID vehicleId,
            TripType tripType,
            BigDecimal offeredAmount
    ) {
    }

    public record PartnerResponseRequest(
            PartnerDecision decision,
            BigDecimal counterAmount
    ) {
    }

    public record CounterResponseRequest(Boolean accept) {
    }

    public record VehicleSummary(
            UUID id,
            String registrationNumber,
            String make,
            String model,
            String category
    ) {
    }

    public record TripSummary(
            String pickup,
            String destination,
            LocalDate startDate,
            LocalDate endDate,
            int passengerCount
    ) {
    }

    public record NegotiationResponse(
            UUID id,
            UUID tripRequestId,
            UUID travelPartnerId,
            String partnerName,
            UUID vehicleId,
            VehicleSummary vehicle,
            TripSummary trip,
            TripType tripType,
            String currency,
            BigDecimal listedAmount,
            BigDecimal offeredAmount,
            BigDecimal counterAmount,
            BigDecimal agreedAmount,
            NegotiationStatus status,
            Instant expiresAt,
            Instant createdAt
    ) {

        static NegotiationResponse from(Negotiation negotiation) {
            NegotiationStatus effective = negotiation.effectiveStatus(Instant.now());

            return new NegotiationResponse(
                    negotiation.getId(),
                    negotiation.getTripRequestId(),
                    negotiation.getTravelPartnerId(),
                    null,
                    negotiation.getVehicleId(),
                    null,
                    null,
                    negotiation.getTripType(),
                    negotiation.getCurrency(),
                    negotiation.getListedAmount(),
                    negotiation.getOfferedAmount(),
                    negotiation.getCounterAmount(),
                    negotiation.isAgreed() ? negotiation.agreedAmount() : null,
                    effective,
                    negotiation.getExpiresAt(),
                    negotiation.getCreatedAt()
            );
        }

        NegotiationResponse withDetails(String partnerName, VehicleSummary vehicle, TripSummary trip) {
            return new NegotiationResponse(
                    id, tripRequestId, travelPartnerId, partnerName, vehicleId, vehicle, trip, tripType,
                    currency, listedAmount, offeredAmount, counterAmount, agreedAmount, status,
                    expiresAt, createdAt
            );
        }
    }
}
