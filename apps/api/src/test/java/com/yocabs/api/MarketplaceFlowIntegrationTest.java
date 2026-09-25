package com.yocabs.api;

import com.yocabs.api.support.MarketplaceFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketplaceFlowIntegrationTest extends MarketplaceFixtures {

    @Test
    void searchNegotiateBookPayAndCompleteATrip() throws Exception {

        Partner partner = createActivePartner("SUV", 7);
        Driver driver = addDriver(partner);
        String tourist = touristToken();

        LocalDate tripDate = today();
        UUID tripRequestId = submittedTripRequest(tourist, tripDate, tripDate, 4);

        // --- search (public) returns the partner's priced option -------------------------------
        MvcResult search =
                post("/api/v1/trip-search", null,
                        "{\"pickup\":{\"description\":\"Bhubaneswar Airport\",\"latitude\":20.2444,\"longitude\":85.8178},"
                                + "\"destination\":{\"description\":\"Puri\",\"latitude\":19.8135,\"longitude\":85.8312},"
                                + "\"startDate\":\"" + tripDate + "\",\"endDate\":\"" + tripDate + "\","
                                + "\"passengerCount\":4,\"vehicleCategory\":\"SUV\","
                                + "\"tripType\":\"CHAUFFEUR_ONE_WAY\"}");

        assertEquals(200, status(search));

        // The route summary the comparison screen shows comes from the backend router.
        BigDecimal distanceKm = decimal(search, "$.distanceKm");
        assertTrue(distanceKm.compareTo(new BigDecimal("30")) > 0 && distanceKm.compareTo(new BigDecimal("120")) < 0);
        assertTrue(((Number) json(search, "$.durationMinutes")).longValue() > 30);

        // A partner without reviews reports a zero rating.
        assertEquals(0, ((Number) ((List<?>) json(search,
                "$.options[?(@.travelPartnerId=='" + partner.id() + "')].reviewCount")).getFirst()).intValue());

        List<Object> options =
                json(search, "$.options[?(@.travelPartnerId=='" + partner.id() + "')].vehicleId");
        assertEquals(1, options.size());

        BigDecimal listed =
                new BigDecimal(String.valueOf(
                        ((List<?>) json(search, "$.options[?(@.travelPartnerId=='" + partner.id() + "')].price.totalAmount")).getFirst()));

        // Real routing, not a hard-coded 100 km: base 600 + 350 allowance + per-km charge.
        assertTrue(listed.compareTo(new BigDecimal("951")) > 0);

        // --- negotiation: offer, counter, accept ----------------------------------------------
        BigDecimal offer = listed.multiply(new BigDecimal("0.80")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal counter = listed.multiply(new BigDecimal("0.90")).setScale(2, RoundingMode.HALF_UP);

        MvcResult started =
                post("/api/v1/trip-requests/" + tripRequestId + "/negotiations", tourist,
                        "{\"vehicleId\":\"" + partner.vehicleId() + "\",\"tripType\":\"CHAUFFEUR_ONE_WAY\","
                                + "\"offeredAmount\":" + offer + "}");
        assertEquals(201, status(started));
        assertEquals(0, listed.compareTo(decimal(started, "$.listedAmount")));

        // The negotiation carries the context the screens display.
        assertEquals("Bhubaneswar Airport", json(started, "$.trip.pickup"));
        assertEquals("Puri", json(started, "$.trip.destination"));
        assertEquals(partner.registrationNumber(), json(started, "$.vehicle.registrationNumber"));
        assertNotNull((Object) json(started, "$.partnerName"));

        UUID negotiationId = UUID.fromString(json(started, "$.id"));

        // The tourist cannot make a second offer to the same partner for the same trip.
        assertEquals(409,
                status(post("/api/v1/trip-requests/" + tripRequestId + "/negotiations", tourist,
                        "{\"vehicleId\":\"" + partner.vehicleId() + "\",\"tripType\":\"CHAUFFEUR_ONE_WAY\","
                                + "\"offeredAmount\":" + offer + "}")));

        MvcResult countered =
                post("/api/v1/negotiations/" + negotiationId + "/respond", partner.ownerToken(),
                        "{\"decision\":\"COUNTER\",\"counterAmount\":" + counter + "}");
        assertEquals("COUNTER_SENT", json(countered, "$.status"));

        MvcResult accepted =
                post("/api/v1/negotiations/" + negotiationId + "/counter-response", tourist,
                        "{\"accept\":true}");
        assertEquals("COUNTER_ACCEPTED", json(accepted, "$.status"));
        assertEquals(0, counter.compareTo(decimal(accepted, "$.agreedAmount")));

        // --- booking at the negotiated price, idempotent on retry ------------------------------
        String key = idempotencyKey();
        MvcResult booking = bookRaw(tourist, tripRequestId, partner.vehicleId(), negotiationId, key);

        assertEquals(201, status(booking));
        assertEquals("PENDING_PAYMENT", json(booking, "$.status"));
        assertEquals(0, counter.compareTo(decimal(booking, "$.totalAmount")));
        assertEquals(0,
                counter.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP)
                        .compareTo(decimal(booking, "$.tokenAmount")));

        UUID bookingId = UUID.fromString(json(booking, "$.id"));

        MvcResult replay = bookRaw(tourist, tripRequestId, partner.vehicleId(), negotiationId, key);
        assertEquals(bookingId.toString(), json(replay, "$.id"));

        // Tourists never see the platform commission; the partner does.
        assertNull((Object) json(booking, "$.commissionAmount"));

        // Names the screens need, without a driver assigned yet and without tourist contact details.
        assertNotNull((Object) json(booking, "$.partnerName"));
        assertEquals(partner.registrationNumber(), json(booking, "$.vehicle.registrationNumber"));
        assertNull((Object) json(booking, "$.driver"));
        assertNull((Object) json(booking, "$.tourist"));

        // --- payment confirms the booking; a duplicate webhook is harmless ---------------------
        UUID paymentId = pay(tourist, bookingId);

        MvcResult confirmed = get("/api/v1/bookings/" + bookingId, tourist);
        assertEquals("CONFIRMED", json(confirmed, "$.status"));

        MvcResult payments = get("/api/v1/bookings/" + bookingId + "/payments", tourist);
        assertEquals("SUCCEEDED", json(payments, "$[0].status"));
        assertEquals(paymentId.toString(), json(payments, "$[0].id"));

        // Both sides were notified.
        assertTrue(body(get("/api/v1/notifications", tourist)).contains("BOOKING_CONFIRMED"));
        assertTrue(body(get("/api/v1/notifications", partner.ownerToken())).contains("BOOKING_RECEIVED"));

        // --- a second tourist cannot double-book the same vehicle for the same date -----------
        String otherTourist = touristToken();
        UUID otherTrip = submittedTripRequest(otherTourist, tripDate, tripDate, 4);
        assertEquals(409, status(bookRaw(otherTourist, otherTrip, partner.vehicleId(), null, idempotencyKey())));

        // --- trip execution: assign driver, start, complete -----------------------------------
        assertEquals(200,
                status(post("/api/v1/bookings/" + bookingId + "/driver", partner.ownerToken(),
                        "{\"driverId\":\"" + driver.id() + "\"}")));

        // Contact details are shared where the trip needs them.
        MvcResult touristView = get("/api/v1/bookings/" + bookingId, tourist);
        assertEquals("Driver", json(touristView, "$.driver.name"));
        assertNotNull((Object) json(touristView, "$.driver.mobile"));
        assertNull((Object) json(touristView, "$.tourist"));

        MvcResult partnerView = get("/api/v1/bookings/" + bookingId, partner.ownerToken());
        assertNotNull((Object) json(partnerView, "$.tourist.mobile"));
        assertNotNull((Object) json(partnerView, "$.driver.name"));

        MvcResult driverView = get("/api/v1/driver/bookings", driver.token());
        assertNotNull((Object) json(driverView, "$[0].tourist.mobile"));
        assertNull((Object) json(driverView, "$[0].driver"));

        assertEquals("IN_PROGRESS",
                json(post("/api/v1/bookings/" + bookingId + "/start", driver.token(), "{}"), "$.status"));

        assertEquals("COMPLETED",
                json(post("/api/v1/bookings/" + bookingId + "/complete", driver.token(), "{}"), "$.status"));

        // --- review and settlement ------------------------------------------------------------
        assertEquals(201,
                status(post("/api/v1/bookings/" + bookingId + "/review", tourist,
                        "{\"rating\":5,\"comment\":\"Great trip\"}")));

        assertEquals(409,
                status(post("/api/v1/bookings/" + bookingId + "/review", tourist, "{\"rating\":4}")));

        MvcResult rating = get("/api/v1/travel-partners/" + partner.id() + "/rating", tourist);
        assertEquals(1, (int) json(rating, "$.reviewCount"));

        // Token (5%) < commission (10%): the partner owes the difference.
        MvcResult wallet = get("/api/v1/travel-partners/" + partner.id() + "/wallet", partner.ownerToken());
        BigDecimal expected =
                counter.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP)
                        .subtract(counter.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP));
        assertEquals(0, expected.compareTo(decimal(wallet, "$.balance")));
        assertEquals("DEBIT", json(wallet, "$.entries[0].type"));
    }

    @Test
    void aPartnerCanReadItsOwnOrganisationAndOthersCannot() throws Exception {

        Partner partner = createActivePartner("SUV", 7);
        Partner other = createActivePartner("SEDAN", 4);

        MvcResult own = get("/api/v1/travel-partners/" + partner.id(), partner.ownerToken());
        assertEquals(200, status(own));
        assertEquals("ACTIVE", json(own, "$.status"));
        MvcResult ownVehicle =
                get("/api/v1/travel-partners/" + partner.id() + "/vehicles/" + partner.vehicleId(),
                        partner.ownerToken());
        assertEquals(1, ((List<?>) json(ownVehicle, "$.serviceAreas")).size());
        assertEquals("Bhubaneswar", json(ownVehicle, "$.serviceAreas[0].name"));

        assertEquals(403, status(get("/api/v1/travel-partners/" + partner.id(), other.ownerToken())));
        assertEquals(403, status(get("/api/v1/travel-partners/" + partner.id(), touristToken())));
        assertEquals(200, status(get("/api/v1/travel-partners/" + partner.id(), adminToken())));
    }

    @Test
    void touristCancellingEarlyIsRefundedAndFreesTheVehicle() throws Exception {

        Partner partner = createActivePartner("SEDAN", 4);
        String tourist = touristToken();

        LocalDate travelDay = today().plusDays(10);
        UUID tripRequestId = submittedTripRequest(tourist, travelDay, travelDay, 3);

        UUID bookingId = book(tourist, tripRequestId, partner.vehicleId(), null);
        UUID paymentId = pay(tourist, bookingId);

        MvcResult cancelled =
                post("/api/v1/bookings/" + bookingId + "/cancel", tourist, "{\"reason\":\"Plans changed\"}");
        assertEquals("CANCELLED", json(cancelled, "$.status"));

        MvcResult payments = get("/api/v1/bookings/" + bookingId + "/payments", tourist);
        assertEquals(paymentId.toString(), json(payments, "$[0].id"));
        assertEquals("REFUNDED", json(payments, "$[0].status"));

        // The vehicle can be booked again for the same date.
        String another = touristToken();
        UUID anotherTrip = submittedTripRequest(another, travelDay, travelDay, 3);
        assertEquals(201, status(bookRaw(another, anotherTrip, partner.vehicleId(), null, idempotencyKey())));
    }

    @Test
    void lateCancellationByTouristIsNotRefunded() throws Exception {

        Partner partner = createActivePartner("SEDAN", 4);
        String tourist = touristToken();

        // Trip starts today: inside the 24h free-cancellation window.
        UUID tripRequestId = submittedTripRequest(tourist, today(), today(), 2);
        UUID bookingId = book(tourist, tripRequestId, partner.vehicleId(), null);
        pay(tourist, bookingId);

        post("/api/v1/bookings/" + bookingId + "/cancel", tourist, "{}");

        MvcResult payments = get("/api/v1/bookings/" + bookingId + "/payments", tourist);
        assertEquals("SUCCEEDED", json(payments, "$[0].status"));
    }

    @Test
    void partnerCancellationAlwaysRefundsTheTourist() throws Exception {

        Partner partner = createActivePartner("SEDAN", 4);
        String tourist = touristToken();

        UUID tripRequestId = submittedTripRequest(tourist, today(), today(), 2);
        UUID bookingId = book(tourist, tripRequestId, partner.vehicleId(), null);
        pay(tourist, bookingId);

        post("/api/v1/bookings/" + bookingId + "/cancel", partner.ownerToken(), "{\"reason\":\"Breakdown\"}");

        assertEquals("REFUNDED",
                json(get("/api/v1/bookings/" + bookingId + "/payments", tourist), "$[0].status"));
        assertTrue(body(get("/api/v1/notifications", tourist)).contains("BOOKING_CANCELLED"));
    }

    @Test
    void anAcceptedNegotiatedPriceCanOnlyBeUsedOnce() throws Exception {

        Partner partner = createActivePartner("SUV", 7);
        String tourist = touristToken();

        UUID first = submittedTripRequest(tourist, today().plusDays(20), today().plusDays(20), 4);

        MvcResult listing = post("/api/v1/trip-search", null,
                "{\"pickup\":{\"description\":\"A\",\"latitude\":20.2444,\"longitude\":85.8178},"
                        + "\"destination\":{\"description\":\"B\",\"latitude\":19.8135,\"longitude\":85.8312},"
                        + "\"startDate\":\"" + today() + "\",\"endDate\":\"" + today() + "\",\"passengerCount\":4}");
        BigDecimal listed =
                new BigDecimal(String.valueOf(
                        ((List<?>) json(listing, "$.options[?(@.travelPartnerId=='" + partner.id() + "')].price.totalAmount")).getFirst()));

        MvcResult negotiation =
                post("/api/v1/trip-requests/" + first + "/negotiations", tourist,
                        "{\"vehicleId\":\"" + partner.vehicleId() + "\",\"tripType\":\"CHAUFFEUR_ONE_WAY\","
                                + "\"offeredAmount\":" + listed.multiply(new BigDecimal("0.9")).setScale(2, RoundingMode.HALF_UP) + "}");
        UUID negotiationId = UUID.fromString(json(negotiation, "$.id"));

        post("/api/v1/negotiations/" + negotiationId + "/respond", partner.ownerToken(),
                "{\"decision\":\"ACCEPT\"}");

        UUID bookingId = book(tourist, first, partner.vehicleId(), negotiationId);
        post("/api/v1/bookings/" + bookingId + "/cancel", tourist, "{}");

        // A booking cannot reuse a negotiation that was already consumed.
        assertEquals(409,
                status(bookRaw(tourist, first, partner.vehicleId(), negotiationId, idempotencyKey())));
    }
}
