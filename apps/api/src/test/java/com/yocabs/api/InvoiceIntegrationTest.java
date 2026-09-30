package com.yocabs.api;

import com.yocabs.api.support.MarketplaceFixtures;
import org.springframework.test.web.servlet.MvcResult;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceIntegrationTest extends MarketplaceFixtures {

    @Test
    void aCompletedTripsBillCanBeOpenedAsALinkButNotBeforeItIsPaidForAndDone() throws Exception {

        Partner partner = createActivePartner("SEDAN", 4);
        Driver driver = addDriver(partner);
        String tourist = touristToken();

        UUID trip = submittedTripRequest(tourist, today(), today(), 4);
        UUID booking = book(tourist, trip, partner.vehicleId(), null);

        // Not payable yet.
        assertEquals(409, status(post("/api/v1/bookings/" + booking + "/invoice-link", tourist, "{}")));

        pay(tourist, booking);

        // Confirmed but not yet completed.
        assertEquals(409, status(post("/api/v1/bookings/" + booking + "/invoice-link", tourist, "{}")));

        completeTrip(partner, driver, tourist, booking);

        // Someone else's booking is refused.
        String stranger = touristToken();
        assertEquals(403, status(post("/api/v1/bookings/" + booking + "/invoice-link", stranger, "{}")));

        MvcResult linkResult = post("/api/v1/bookings/" + booking + "/invoice-link", tourist, "{}");
        assertEquals(200, status(linkResult));
        String url = json(linkResult, "$.url").toString();
        assertTrue(url.contains("/api/v1/bookings/invoice?token="));

        String path = url.substring(url.indexOf("/api/v1/bookings/invoice"));

        // The bill opens with no sign-in at all: that is the whole point of the link.
        MvcResult pdf = get(path, null);
        assertEquals(200, status(pdf));
        assertEquals("application/pdf", pdf.getResponse().getContentType());
        byte[] bytes = pdf.getResponse().getContentAsByteArray();
        assertTrue(bytes.length > 200);
        // A PDF file always starts with this magic header.
        assertEquals("%PDF", new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));

        // A made-up or tampered token is refused.
        assertEquals(404, status(get("/api/v1/bookings/invoice?token=not-a-real-token", null)));
        assertEquals(404, status(get(path + "x", null)));

        // The partner and an admin can also get a link for the same booking.
        assertEquals(200, status(post("/api/v1/bookings/" + booking + "/invoice-link", partner.ownerToken(), "{}")));
        assertEquals(200, status(post("/api/v1/bookings/" + booking + "/invoice-link", adminToken(), "{}")));
    }
}
