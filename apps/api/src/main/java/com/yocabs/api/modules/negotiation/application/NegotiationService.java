package com.yocabs.api.modules.negotiation.application;

import com.yocabs.api.modules.negotiation.domain.model.Negotiation;
import com.yocabs.api.modules.negotiation.domain.model.NegotiationStatus;
import com.yocabs.api.modules.negotiation.domain.repository.NegotiationRepository;
import com.yocabs.api.modules.pricing.application.model.PricedTravelOption;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.model.TripRequestStatus;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.triprequest.domain.repository.TripRequestRepository;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripRequestId;
import com.yocabs.api.modules.tripsearch.application.SelectedOptionService;
import com.yocabs.api.shared.events.NotificationRequested;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NegotiationService {

    private static final Logger log = LoggerFactory.getLogger(NegotiationService.class);

    public enum PartnerDecision {
        ACCEPT,
        REJECT,
        COUNTER
    }

    private final NegotiationRepository negotiations;
    private final TripRequestRepository tripRequests;
    private final SelectedOptionService selectedOptionService;
    private final ApplicationEventPublisher events;
    private final Duration validity;

    public NegotiationService(
            NegotiationRepository negotiations,
            TripRequestRepository tripRequests,
            SelectedOptionService selectedOptionService,
            ApplicationEventPublisher events,
            @Value("${yocabs.negotiation.expiry-minutes:60}") long expiryMinutes
    ) {
        this.negotiations = negotiations;
        this.tripRequests = tripRequests;
        this.selectedOptionService = selectedOptionService;
        this.events = events;
        this.validity = Duration.ofMinutes(expiryMinutes);
    }

    @Transactional
    public Negotiation startNegotiation(
            Actor actor,
            UUID tripRequestId,
            UUID vehicleId,
            TripType tripType,
            BigDecimal offeredAmount
    ) {
        actor.requireRole(Role.TOURIST);

        if (offeredAmount == null) {
            throw new IllegalArgumentException("Offer is required");
        }

        TripRequest tripRequest = loadOwnedSubmittedTripRequest(actor, tripRequestId);

        // Re-price server-side: the listed price is never taken from the client.
        PricedTravelOption option =
                selectedOptionService.resolve(tripRequest, vehicleId, tripType);

        UUID partnerId = option.travelPartner().getId();

        negotiations.findByTripRequestIdAndPartnerId(tripRequestId, partnerId)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "You have already negotiated with this travel partner for this trip"
                    );
                });

        Negotiation negotiation =
                negotiations.create(
                        Negotiation.start(
                                tripRequestId,
                                actor.userId(),
                                partnerId,
                                vehicleId,
                                tripType,
                                option.price().currency(),
                                option.price().totalAmount(),
                                offeredAmount.setScale(2, RoundingMode.HALF_UP),
                                validity
                        )
                );

        events.publishEvent(
                NotificationRequested.toPartner(
                        partnerId,
                        "NEGOTIATION_OFFER_RECEIVED",
                        "New price offer",
                        "A tourist offered " + negotiation.getCurrency() + " "
                                + negotiation.getOfferedAmount() + " for a trip. Respond before it expires.",
                        "NEGOTIATION",
                        negotiation.getId()
                )
        );

        return negotiation;
    }

    @Transactional
    public Negotiation partnerRespond(
            Actor actor,
            UUID negotiationId,
            PartnerDecision decision,
            BigDecimal counterAmount
    ) {
        if (decision == null) {
            throw new IllegalArgumentException("Decision is required");
        }

        Negotiation negotiation = load(negotiationId);

        if (!actor.isPartnerUser()) {
            throw new AccessDeniedException("Only the travel partner can respond to an offer");
        }
        actor.requirePartnerAccess(negotiation.getTravelPartnerId());

        Instant now = Instant.now();

        switch (decision) {
            case ACCEPT -> negotiation.partnerAccepts(now);
            case REJECT -> negotiation.partnerRejects(now);
            case COUNTER -> {
                if (counterAmount == null) {
                    throw new IllegalArgumentException("Counter amount is required");
                }
                negotiation.partnerCounters(
                        counterAmount.setScale(2, RoundingMode.HALF_UP), now, validity
                );
            }
        }

        Negotiation saved = negotiations.update(negotiation);

        events.publishEvent(
                NotificationRequested.toUser(
                        saved.getTouristId(),
                        "NEGOTIATION_" + saved.getStatus(),
                        "Update on your price offer",
                        describeOutcome(saved),
                        "NEGOTIATION",
                        saved.getId()
                )
        );

        return saved;
    }

    @Transactional
    public Negotiation touristRespondToCounter(Actor actor, UUID negotiationId, boolean accept) {

        Negotiation negotiation = load(negotiationId);
        actor.requireTouristIs(negotiation.getTouristId());

        Instant now = Instant.now();

        if (accept) {
            negotiation.touristAcceptsCounter(now);
        } else {
            negotiation.touristRejectsCounter(now);
        }

        Negotiation saved = negotiations.update(negotiation);

        events.publishEvent(
                NotificationRequested.toPartner(
                        saved.getTravelPartnerId(),
                        "NEGOTIATION_" + saved.getStatus(),
                        "Counter offer " + (accept ? "accepted" : "declined"),
                        "The tourist " + (accept ? "accepted" : "declined") + " your counter offer.",
                        "NEGOTIATION",
                        saved.getId()
                )
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public Negotiation get(Actor actor, UUID negotiationId) {

        Negotiation negotiation = load(negotiationId);
        requireParticipant(actor, negotiation);
        return negotiation;
    }

    @Transactional(readOnly = true)
    public List<Negotiation> listForTripRequest(Actor actor, UUID tripRequestId) {

        TripRequest tripRequest = loadTripRequest(tripRequestId);

        if (!actor.isAdmin()) {
            actor.requireTouristIs(tripRequest.getTouristId().value());
        }

        return negotiations.findByTripRequestId(tripRequestId);
    }

    @Transactional(readOnly = true)
    public List<Negotiation> listForPartner(Actor actor, UUID travelPartnerId, NegotiationStatus status) {

        actor.requirePartnerAccess(travelPartnerId);
        return negotiations.findByPartnerId(travelPartnerId, status);
    }

    /**
     * Called by booking: validates that the accepted negotiation matches the
     * chosen option and burns it so it can be used for exactly one booking.
     */
    @Transactional
    public BigDecimal consumeAgreedPrice(
            UUID negotiationId,
            UUID touristId,
            UUID tripRequestId,
            UUID vehicleId,
            TripType tripType
    ) {
        Negotiation negotiation = load(negotiationId);

        if (!negotiation.getTouristId().equals(touristId)
                || !negotiation.getTripRequestId().equals(tripRequestId)
                || !negotiation.getVehicleId().equals(vehicleId)
                || negotiation.getTripType() != tripType) {
            throw new IllegalArgumentException(
                    "The negotiation does not match the selected trip option"
            );
        }

        if (!negotiation.isAgreed()) {
            throw new IllegalStateException("The negotiation has no agreed price");
        }

        negotiation.markConsumed(Instant.now());
        negotiations.update(negotiation);

        return negotiation.agreedAmount();
    }

    @Scheduled(fixedDelayString = "${yocabs.negotiation.expiry-scan-ms:60000}")
    @Transactional
    public void expireDueNegotiations() {

        Instant now = Instant.now();
        int expired = 0;

        for (Negotiation negotiation : negotiations.findOpenDueForExpiry(now)) {
            if (negotiation.expireIfDue(now)) {
                negotiations.update(negotiation);
                expired++;
            }
        }

        if (expired > 0) {
            log.info("Expired {} negotiation(s)", expired);
        }
    }

    private void requireParticipant(Actor actor, Negotiation negotiation) {
        if (actor.isAdmin()) {
            return;
        }
        if (actor.role() == Role.TOURIST) {
            actor.requireTouristIs(negotiation.getTouristId());
            return;
        }
        actor.requirePartnerAccess(negotiation.getTravelPartnerId());
    }

    private String describeOutcome(Negotiation negotiation) {
        return switch (negotiation.getStatus()) {
            case ACCEPTED -> "Your offer was accepted. You can now book at the agreed price.";
            case REJECTED -> "Your offer was declined. You can still book at the listed price.";
            case COUNTER_SENT -> "The travel partner sent a counter offer of "
                    + negotiation.getCurrency() + " " + negotiation.getCounterAmount() + ".";
            default -> "Your offer was updated.";
        };
    }

    private Negotiation load(UUID negotiationId) {
        return negotiations.findById(negotiationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Negotiation not found: " + negotiationId));
    }

    private TripRequest loadTripRequest(UUID tripRequestId) {
        return tripRequests.findById(new TripRequestId(tripRequestId))
                .orElseThrow(() ->
                        new ResourceNotFoundException("Trip request not found: " + tripRequestId));
    }

    private TripRequest loadOwnedSubmittedTripRequest(Actor actor, UUID tripRequestId) {

        TripRequest tripRequest = loadTripRequest(tripRequestId);
        actor.requireTouristIs(tripRequest.getTouristId().value());

        if (tripRequest.getStatus() != TripRequestStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only a submitted trip request can be negotiated"
            );
        }

        return tripRequest;
    }
}
