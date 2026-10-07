package com.yocabs.api;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TouristFeedbackIntegrationTest extends com.yocabs.api.support.MarketplaceFixtures {

    @Test
    void driverCanRateTheTravellerOnlyAfterTheTripIsCompletedAndPaidInFull() throws Exception {
        Partner partner = createActivePartner("SEDAN", 4);
        Driver driver = addDriver(partner);
        String tourist = touristToken();

        UUID tripRequestId = submittedTripRequest(tourist, today(), today(), 2);
        UUID bookingId = book(tourist, tripRequestId, partner.vehicleId(), null);
        pay(tourist, bookingId);
        completeTrip(partner, driver, tourist, bookingId);

        // The balance (fare less the token already paid) is still owed.
        assertEquals(false,
                json(get("/api/v1/bookings/" + bookingId, driver.token()), "$.balanceSettled"));
        assertEquals(409,
                status(post("/api/v1/bookings/" + bookingId + "/tourist-feedback", driver.token(),
                        "{\"rating\":5}")));

        // The tourist pays the balance online.
        MvcResult balancePayment = post("/api/v1/bookings/" + bookingId + "/payments/balance", tourist, "{}");
        UUID balancePaymentId = UUID.fromString(json(balancePayment, "$.id"));
        post("/api/v1/dev/payments/" + balancePaymentId + "/simulate", tourist, "{\"outcome\":\"SUCCESS\"}");

        assertEquals(true,
                json(get("/api/v1/bookings/" + bookingId, driver.token()), "$.balanceSettled"));
        assertEquals(false,
                json(get("/api/v1/bookings/" + bookingId, driver.token()), "$.touristRated"));

        assertEquals(201,
                status(post("/api/v1/bookings/" + bookingId + "/tourist-feedback", driver.token(),
                        "{\"rating\":5,\"comment\":\"Pleasant traveller\"}")));

        assertEquals(true,
                json(get("/api/v1/bookings/" + bookingId, driver.token()), "$.touristRated"));

        // Cannot rate the same trip twice.
        assertEquals(409,
                status(post("/api/v1/bookings/" + bookingId + "/tourist-feedback", driver.token(),
                        "{\"rating\":3}")));

        // Driver-only fields: a tourist looking at their own booking never sees them flip.
        assertEquals(false, json(get("/api/v1/bookings/" + bookingId, tourist), "$.balanceSettled"));
        assertEquals(false, json(get("/api/v1/bookings/" + bookingId, tourist), "$.touristRated"));
    }

    @Test
    void onlyTheAssignedDriverCanSubmitTouristFeedback() throws Exception {
        Partner partner = createActivePartner("SEDAN", 4);
        Driver driver = addDriver(partner);
        String tourist = touristToken();

        UUID tripRequestId = submittedTripRequest(tourist, today(), today(), 2);
        UUID bookingId = book(tourist, tripRequestId, partner.vehicleId(), null);
        pay(tourist, bookingId);
        completeTrip(partner, driver, tourist, bookingId);

        assertEquals(403,
                status(post("/api/v1/bookings/" + bookingId + "/tourist-feedback", tourist, "{\"rating\":5}")));
        assertEquals(403,
                status(post("/api/v1/bookings/" + bookingId + "/tourist-feedback", partner.ownerToken(),
                        "{\"rating\":5}")));
    }
}
