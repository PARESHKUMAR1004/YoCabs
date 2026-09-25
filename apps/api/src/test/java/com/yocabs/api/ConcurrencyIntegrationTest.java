package com.yocabs.api;

import com.yocabs.api.modules.payment.infrastructure.SandboxPaymentGateway;
import com.yocabs.api.support.MarketplaceFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves the double-booking and duplicate-event protections hold under real parallelism. */
class ConcurrencyIntegrationTest extends MarketplaceFixtures {

    @Autowired
    private SandboxPaymentGateway gateway;

    @Autowired
    private JdbcTemplate jdbc;

    private <T> List<T> runInParallel(List<Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);

        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return task.call();
                }));
            }

            ready.await();
            go.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void exactlyOneOfManySimultaneousBookingsForTheSameVehicleAndDateWins() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        LocalDate day = today().plusDays(70);

        int contenders = 8;
        List<String> tourists = new ArrayList<>();
        List<UUID> trips = new ArrayList<>();
        for (int i = 0; i < contenders; i++) {
            String tourist = touristToken();
            tourists.add(tourist);
            trips.add(submittedTripRequest(tourist, day, day, 3));
        }

        List<Callable<Integer>> attempts = new ArrayList<>();
        for (int i = 0; i < contenders; i++) {
            String tourist = tourists.get(i);
            UUID trip = trips.get(i);
            attempts.add(() -> status(bookRaw(tourist, trip, partner.vehicleId(), null, idempotencyKey())));
        }

        List<Integer> statuses = runInParallel(attempts);

        long winners = statuses.stream().filter(code -> code == 201).count();
        long losers = statuses.stream().filter(code -> code == 409).count();

        assertEquals(1, winners, "statuses: " + statuses);
        assertEquals(contenders - 1, losers, "statuses: " + statuses);

        Integer live =
                jdbc.queryForObject(
                        "select count(*) from bookings where vehicle_id = ? and start_date = ? "
                                + "and status in ('PENDING_PAYMENT','CONFIRMED')",
                        Integer.class, partner.vehicleId(), java.sql.Date.valueOf(day));
        assertEquals(1, live);
    }

    @Test
    void aTouristRetryingConcurrentlyWithTheSameKeyCreatesOneBooking() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String tourist = touristToken();
        LocalDate day = today().plusDays(71);
        UUID trip = submittedTripRequest(tourist, day, day, 3);
        String key = idempotencyKey();

        List<Callable<Integer>> attempts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            attempts.add(() -> status(bookRaw(tourist, trip, partner.vehicleId(), null, key)));
        }

        List<Integer> statuses = runInParallel(attempts);

        assertTrue(statuses.stream().allMatch(code -> code == 201 || code == 409), "statuses: " + statuses);
        assertTrue(statuses.contains(201), "statuses: " + statuses);

        Integer bookings =
                jdbc.queryForObject(
                        "select count(*) from bookings where trip_request_id = ?", Integer.class, trip);
        assertEquals(1, bookings);
    }

    @Test
    void theSamePaymentEventDeliveredInParallelIsAppliedOnce() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String tourist = touristToken();
        LocalDate day = today().plusDays(72);
        UUID trip = submittedTripRequest(tourist, day, day, 3);
        UUID bookingId = book(tourist, trip, partner.vehicleId(), null);

        UUID paymentId =
                UUID.fromString(json(post("/api/v1/bookings/" + bookingId + "/payments", tourist, "{}"), "$.id"));
        String orderId = jdbc.queryForObject("select gateway_order_id from payments where id = ?", String.class, paymentId);
        String amount = jdbc.queryForObject("select amount::text from payments where id = ?", String.class, paymentId);

        String payload =
                "{\"eventId\":\"evt_" + UUID.randomUUID() + "\",\"type\":\"PAYMENT_SUCCEEDED\",\"orderId\":\""
                        + orderId + "\",\"paymentId\":\"pay_" + UUID.randomUUID().toString().replace("-", "")
                        + "\",\"amount\":\"" + amount + "\"}";
        String signature = gateway.sign(payload);

        List<Callable<Integer>> deliveries = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            deliveries.add(() -> status(perform(
                    MockMvcRequestBuilders.post("/api/v1/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Signature", signature)
                            .content(payload))));
        }

        List<Integer> statuses = runInParallel(deliveries);

        // Each delivery is either applied, recognised as a duplicate, or asks the sender to retry.
        assertTrue(statuses.stream().allMatch(code -> code == 200 || code == 409), "statuses: " + statuses);
        assertTrue(statuses.contains(200), "statuses: " + statuses);

        assertEquals(1, jdbc.queryForObject(
                "select count(*) from payment_transactions where payment_id = ?", Integer.class, paymentId));
        assertEquals("SUCCEEDED", jdbc.queryForObject(
                "select status from payments where id = ?", String.class, paymentId));
        assertEquals("CONFIRMED", json(get("/api/v1/bookings/" + bookingId, tourist), "$.status"));
    }
}
