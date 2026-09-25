package com.yocabs.api.modules.negotiation.interfaces.rest;

import com.yocabs.api.modules.negotiation.domain.model.Negotiation;
import com.yocabs.api.modules.negotiation.interfaces.rest.NegotiationController.NegotiationResponse;
import com.yocabs.api.modules.negotiation.interfaces.rest.NegotiationController.TripSummary;
import com.yocabs.api.modules.negotiation.interfaces.rest.NegotiationController.VehicleSummary;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.repository.TripRequestRepository;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripRequestId;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** Adds the partner, vehicle and trip context the negotiation screens display. */
@Component
public class NegotiationViewAssembler {

    private final TravelPartnerRepository partners;
    private final VehicleRepository vehicles;
    private final TripRequestRepository tripRequests;

    public NegotiationViewAssembler(
            TravelPartnerRepository partners,
            VehicleRepository vehicles,
            TripRequestRepository tripRequests
    ) {
        this.partners = partners;
        this.vehicles = vehicles;
        this.tripRequests = tripRequests;
    }

    @Transactional(readOnly = true)
    public NegotiationResponse toResponse(Negotiation negotiation) {
        return toResponses(List.of(negotiation)).getFirst();
    }

    @Transactional(readOnly = true)
    public List<NegotiationResponse> toResponses(List<Negotiation> negotiations) {

        Map<UUID, Optional<TravelPartner>> partnerCache = new HashMap<>();
        Map<UUID, Optional<Vehicle>> vehicleCache = new HashMap<>();
        Map<UUID, Optional<TripRequest>> tripCache = new HashMap<>();

        return negotiations.stream()
                .map(negotiation ->
                        NegotiationResponse.from(negotiation).withDetails(
                                cached(partnerCache, negotiation.getTravelPartnerId(), partners::findById)
                                        .map(TravelPartner::getName).orElse(null),
                                cached(vehicleCache, negotiation.getVehicleId(), vehicles::findById)
                                        .map(this::summarize).orElse(null),
                                cached(tripCache, negotiation.getTripRequestId(),
                                        id -> tripRequests.findById(new TripRequestId(id)))
                                        .map(this::summarize).orElse(null)
                        ))
                .toList();
    }

    private VehicleSummary summarize(Vehicle vehicle) {
        return new VehicleSummary(
                vehicle.getId(), vehicle.getRegistrationNumber(), vehicle.getMake(),
                vehicle.getModel(), vehicle.getCategory().name()
        );
    }

    private TripSummary summarize(TripRequest trip) {
        return new TripSummary(
                trip.getItinerary().pickup().description(),
                trip.getItinerary().destination().description(),
                trip.getTravelDateRange().startDate(),
                trip.getTravelDateRange().endDate(),
                trip.getPassengerCount().value()
        );
    }

    private static <T> Optional<T> cached(
            Map<UUID, Optional<T>> cache,
            UUID id,
            Function<UUID, Optional<T>> loader
    ) {
        return cache.computeIfAbsent(id, loader);
    }
}
