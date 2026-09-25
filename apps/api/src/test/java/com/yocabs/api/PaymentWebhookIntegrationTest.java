package com.yocabs.api;

import com.yocabs.api.modules.booking.application.BookingService;
import com.yocabs.api.modules.payment.infrastructure.SandboxPaymentGateway;
import com.yocabs.api.support.MarketplaceFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentWebhookIntegrationTest extends MarketplaceFixtures {

    @Autowired
    private SandboxPaymentGateway gateway;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private BookingService bookingService;

    private MvcResult webhook(String payload, String signature) throws Exception {
        return perform(
                MockMvcRequestBuilders.post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Signature", signature)
                        .content(payload));
    }

    private String event(String eventId, String type, String orderId, String amount) {
        return "{\"eventId\":\"" + eventId + "\",\"type\":\"" + type + "\",\"orderId\":\"" + orderId
                + "\",\"paymentId\":\"pay_" + UUID.randomUUID().toString().replace("-", "")
                + "\",\"amount\":\"" + amount + "\"}";
    }

    private String orderIdOf(UUID paymentId) {
        return jdbc.queryForObject(
                "select gateway_order_id from payments where id = ?", String.class, paymentId);
    }

    private String amountOf(UUID paymentId) {
        return jdbc.queryForObject(
                "select amount::text from payments where id = ?", String.class, paymentId);
    }

    private UUID initiate(String tourist, UUID bookingId) throws Exception {
        return UUID.fromString(json(post("/api/v1/bookings/" + bookingId + "/payments", tourist, "{}"), "$.id"));
    }

    @Test
    void aWebhookWithABadSignatureIsRejectedAndChangesNothing() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String tourist = touristToken();
        UUID trip = submittedTripRequest(tourist, today().plusDays(40), today().plusDays(40), 3);
        UUID bookingId = book(tourist, trip, partner.vehicleId(), null);
        UUID paymentId = initiate(tourist, bookingId);

        String payload = event("evt_" + UUID.randomUUID(), "PAYMENT_SUCCEEDED",
                orderIdOf(paymentId), amountOf(paymentId));

        assertEquals(401, status(webhook(payload, "deadbeef")));
        assertEquals("PENDING_PAYMENT", json(get("/api/v1/bookings/" + bookingId, tourist), "$.status"));
    }

    @Test
    void theSameGatewayEventIsAppliedOnlyOnce() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String tourist = touristToken();
        UUID trip = submittedTripRequest(tourist, today().plusDays(41), today().plusDays(41), 3);
        UUID bookingId = book(tourist, trip, partner.vehicleId(), null);
        UUID paymentId = initiate(tourist, bookingId);

        String payload = event("evt_" + UUID.randomUUID(), "PAYMENT_SUCCEEDED",
                orderIdOf(paymentId), amountOf(paymentId));

        assertEquals(200, status(webhook(payload, gateway.sign(payload))));
        assertEquals(200, status(webhook(payload, gateway.sign(payload))));
        assertEquals(200, status(webhook(payload, gateway.sign(payload))));

        Integer transactions =
                jdbc.queryForObject(
                        "select count(*) from payment_transactions where payment_id = ?",
                        Integer.class, paymentId);

        assertEquals(1, transactions);
        assertEquals("CONFIRMED", json(get("/api/v1/bookings/" + bookingId, tourist), "$.status"));
    }

    @Test
    void anAmountMismatchIsRefusedAndTheBookingStaysUnpaid() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String tourist = touristToken();
        UUID trip = submittedTripRequest(tourist, today().plusDays(42), today().plusDays(42), 3);
        UUID bookingId = book(tourist, trip, partner.vehicleId(), null);
        UUID paymentId = initiate(tourist, bookingId);

        String payload = event("evt_" + UUID.randomUUID(), "PAYMENT_SUCCEEDED", orderIdOf(paymentId), "1.00");

        assertEquals(400, status(webhook(payload, gateway.sign(payload))));
        assertEquals("PENDING_PAYMENT", json(get("/api/v1/bookings/" + bookingId, tourist), "$.status"));
    }

    @Test
    void aFailedPaymentCanBeRetried() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String tourist = touristToken();
        UUID trip = submittedTripRequest(tourist, today().plusDays(43), today().plusDays(43), 3);
        UUID bookingId = book(tourist, trip, partner.vehicleId(), null);

        UUID first = initiate(tourist, bookingId);
        post("/api/v1/dev/payments/" + first + "/simulate", tourist, "{\"outcome\":\"FAILURE\"}");

        assertEquals("PENDING_PAYMENT", json(get("/api/v1/bookings/" + bookingId, tourist), "$.status"));
        assertTrue(body(get("/api/v1/notifications", tourist)).contains("PAYMENT_FAILED"));

        UUID second = initiate(tourist, bookingId);
        assertTrue(!first.equals(second));
        post("/api/v1/dev/payments/" + second + "/simulate", tourist, "{\"outcome\":\"SUCCESS\"}");

        assertEquals("CONFIRMED", json(get("/api/v1/bookings/" + bookingId, tourist), "$.status"));
    }

    @Test
    void unpaidHoldsExpireAndReleaseTheVehicle() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String first = touristToken();
        String second = touristToken();
        var day = today().plusDays(44);

        UUID firstTrip = submittedTripRequest(first, day, day, 3);
        UUID secondTrip = submittedTripRequest(second, day, day, 3);

        UUID firstBooking = book(first, firstTrip, partner.vehicleId(), null);

        // While the hold is live the vehicle is reserved.
        assertEquals(409, status(bookRaw(second, secondTrip, partner.vehicleId(), null, idempotencyKey())));

        jdbc.update("update bookings set hold_expires_at = now() - interval '1 minute' where id = ?", firstBooking);
        bookingService.expireStaleHolds();

        assertEquals("EXPIRED", json(get("/api/v1/bookings/" + firstBooking, first), "$.status"));
        assertEquals(201, status(bookRaw(second, secondTrip, partner.vehicleId(), null, idempotencyKey())));
    }

    @Test
    void aLatePaymentRevivesTheBookingWhenTheVehicleIsStillFree() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String tourist = touristToken();
        UUID trip = submittedTripRequest(tourist, today().plusDays(45), today().plusDays(45), 3);
        UUID bookingId = book(tourist, trip, partner.vehicleId(), null);
        UUID paymentId = initiate(tourist, bookingId);

        jdbc.update("update bookings set hold_expires_at = now() - interval '1 minute' where id = ?", bookingId);
        bookingService.expireStaleHolds();

        post("/api/v1/dev/payments/" + paymentId + "/simulate", tourist, "{\"outcome\":\"SUCCESS\"}");

        assertEquals("CONFIRMED", json(get("/api/v1/bookings/" + bookingId, tourist), "$.status"));
    }

    @Test
    void aLatePaymentIsRefundedWhenSomeoneElseTookTheVehicle() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String slow = touristToken();
        String quick = touristToken();
        var day = today().plusDays(46);

        UUID slowTrip = submittedTripRequest(slow, day, day, 3);
        UUID quickTrip = submittedTripRequest(quick, day, day, 3);

        UUID slowBooking = book(slow, slowTrip, partner.vehicleId(), null);
        UUID slowPayment = initiate(slow, slowBooking);

        jdbc.update("update bookings set hold_expires_at = now() - interval '1 minute' where id = ?", slowBooking);
        bookingService.expireStaleHolds();

        UUID quickBooking = book(quick, quickTrip, partner.vehicleId(), null);
        pay(quick, quickBooking);
        assertEquals("CONFIRMED", json(get("/api/v1/bookings/" + quickBooking, quick), "$.status"));

        // The slow tourist's money arrives now: the booking cannot be honoured, so it is refunded.
        post("/api/v1/dev/payments/" + slowPayment + "/simulate", slow, "{\"outcome\":\"SUCCESS\"}");

        assertEquals("EXPIRED", json(get("/api/v1/bookings/" + slowBooking, slow), "$.status"));
        assertEquals("REFUNDED", json(get("/api/v1/bookings/" + slowBooking + "/payments", slow), "$[0].status"));
        assertTrue(body(get("/api/v1/notifications", slow)).contains("PAYMENT_REFUNDED"));
    }
}
