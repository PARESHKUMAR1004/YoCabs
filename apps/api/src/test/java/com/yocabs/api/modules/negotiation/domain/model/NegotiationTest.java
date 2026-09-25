package com.yocabs.api.modules.negotiation.domain.model;

import com.yocabs.api.modules.triprequest.domain.model.TripType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NegotiationTest {

    private static final Duration VALIDITY = Duration.ofMinutes(60);

    private Negotiation negotiation(String offer) {
        return Negotiation.start(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                TripType.CHAUFFEUR_ONE_WAY, "INR",
                new BigDecimal("1000.00"), new BigDecimal(offer), VALIDITY
        );
    }

    @Test
    void anOfferMustBeBelowTheListedPrice() {
        assertThrows(IllegalArgumentException.class, () -> negotiation("1000.00"));
        assertThrows(IllegalArgumentException.class, () -> negotiation("1200.00"));
        assertThrows(IllegalArgumentException.class, () -> negotiation("0"));
        assertEquals(NegotiationStatus.OFFER_SENT, negotiation("800.00").getStatus());
    }

    @Test
    void thePartnerCanAcceptAndTheOfferBecomesTheAgreedPrice() {
        Negotiation negotiation = negotiation("800.00");

        negotiation.partnerAccepts(Instant.now());

        assertTrue(negotiation.isAgreed());
        assertEquals(0, new BigDecimal("800.00").compareTo(negotiation.agreedAmount()));
    }

    @Test
    void aCounterMustLieStrictlyBetweenTheOfferAndTheListedPrice() {
        Instant now = Instant.now();

        assertThrows(IllegalArgumentException.class,
                () -> negotiation("800.00").partnerCounters(new BigDecimal("800.00"), now, VALIDITY));
        assertThrows(IllegalArgumentException.class,
                () -> negotiation("800.00").partnerCounters(new BigDecimal("700.00"), now, VALIDITY));
        assertThrows(IllegalArgumentException.class,
                () -> negotiation("800.00").partnerCounters(new BigDecimal("1000.00"), now, VALIDITY));

        Negotiation valid = negotiation("800.00");
        valid.partnerCounters(new BigDecimal("900.00"), now, VALIDITY);
        assertEquals(NegotiationStatus.COUNTER_SENT, valid.getStatus());
    }

    @Test
    void theTouristCanAcceptTheCounterAtTheCounterPrice() {
        Negotiation negotiation = negotiation("800.00");
        Instant now = Instant.now();

        negotiation.partnerCounters(new BigDecimal("900.00"), now, VALIDITY);
        negotiation.touristAcceptsCounter(now);

        assertEquals(NegotiationStatus.COUNTER_ACCEPTED, negotiation.getStatus());
        assertEquals(0, new BigDecimal("900.00").compareTo(negotiation.agreedAmount()));
    }

    @Test
    void thereIsNoSecondRound() {
        Negotiation negotiation = negotiation("800.00");
        Instant now = Instant.now();

        negotiation.partnerCounters(new BigDecimal("900.00"), now, VALIDITY);
        negotiation.touristRejectsCounter(now);

        assertEquals(NegotiationStatus.COUNTER_REJECTED, negotiation.getStatus());
        assertThrows(IllegalStateException.class,
                () -> negotiation.partnerCounters(new BigDecimal("850.00"), now, VALIDITY));
        assertThrows(IllegalStateException.class, () -> negotiation.touristAcceptsCounter(now));
    }

    @Test
    void theTouristCannotRespondToACounterThatWasNeverSent() {
        assertThrows(IllegalStateException.class,
                () -> negotiation("800.00").touristAcceptsCounter(Instant.now()));
    }

    @Test
    void actionsAfterExpiryAreRefusedAndTheStatusReadsExpired() {
        Negotiation negotiation = negotiation("800.00");
        Instant later = Instant.now().plus(VALIDITY).plusSeconds(1);

        assertThrows(IllegalStateException.class, () -> negotiation.partnerAccepts(later));
        assertEquals(NegotiationStatus.EXPIRED, negotiation.effectiveStatus(later));

        assertTrue(negotiation.expireIfDue(later));
        assertEquals(NegotiationStatus.EXPIRED, negotiation.getStatus());
        assertFalse(negotiation.expireIfDue(later));
    }

    @Test
    void anAgreedPriceCanBeConsumedExactlyOnce() {
        Negotiation negotiation = negotiation("800.00");
        Instant now = Instant.now();

        assertThrows(IllegalStateException.class, () -> negotiation.markConsumed(now));

        negotiation.partnerAccepts(now);
        negotiation.markConsumed(now);

        assertThrows(IllegalStateException.class, () -> negotiation.markConsumed(now));
    }
}
