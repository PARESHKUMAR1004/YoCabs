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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportsAndFinanceIntegrationTest extends MarketplaceFixtures {

    /** One completed (and reviewed) trip and one cancelled trip for the same partner. */
    private record Scenario(
            Partner partner,
            Driver driver,
            UUID completedBooking,
            BigDecimal completedTotal,
            UUID cancelledBooking,
            UUID cancelledPayment,
            String tourist
    ) {
    }

    private Scenario scenario() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        Driver driver = addDriver(partner);
        String tourist = touristToken();

        UUID trip = submittedTripRequest(tourist, today(), today(), 4);
        UUID completed = book(tourist, trip, partner.vehicleId(), null);
        BigDecimal total = decimal(get("/api/v1/bookings/" + completed, tourist), "$.totalAmount");
        pay(tourist, completed);
        completeTrip(partner, driver, completed);
        post("/api/v1/bookings/" + completed + "/review", tourist, "{\"rating\":5,\"comment\":\"Great\"}");

        // A different trip on another day, cancelled by the partner (always refunded).
        String other = touristToken();
        LocalDate later = today().plusDays(3);
        UUID cancelledTrip = submittedTripRequest(other, later, later, 3);
        UUID cancelled = book(other, cancelledTrip, partner.vehicleId(), null);
        UUID payment = pay(other, cancelled);
        post("/api/v1/bookings/" + cancelled + "/cancel", partner.ownerToken(), "{\"reason\":\"Breakdown\"}");

        return new Scenario(partner, driver, completed, total, cancelled, payment, tourist);
    }

    // ---- partner reports -----------------------------------------------------------------

    @Test
    void earningsReportSummarisesCompletedTripsAndTheWallet() throws Exception {
        Scenario s = scenario();
        String base = "/api/v1/travel-partners/" + s.partner().id() + "/reports";

        MvcResult report = get(base + "/earnings?from=" + today().minusDays(1) + "&to=" + today().plusDays(5),
                s.partner().ownerToken());

        assertEquals(200, status(report));
        assertEquals(1, ((Number) json(report, "$.completedTrips")).intValue());
        assertEquals(0, s.completedTotal().compareTo(decimal(report, "$.grossFare")));

        BigDecimal commission = s.completedTotal().multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, commission.compareTo(decimal(report, "$.commission")));
        assertEquals(0, s.completedTotal().subtract(commission).compareTo(decimal(report, "$.partnerEarnings")));

        // Token (25%) > commission (10%): the ledger shows what YoCabs owes the partner.
        assertTrue(decimal(report, "$.walletBalance").signum() > 0);
        assertTrue(((List<?>) json(report, "$.months")).size() >= 1);

        // Money figures are for the owner (and admins), not for staff or other partners.
        String staffMobile = randomMobile();
        post("/api/v1/travel-partners/" + s.partner().id() + "/staff", s.partner().ownerToken(),
                "{\"name\":\"Clerk\",\"mobile\":\"" + staffMobile + "\"}");
        String staff = otpLogin(staffMobile);

        assertEquals(403, status(get(base + "/earnings", staff)));
        assertEquals(200, status(get(base + "/trips", staff)));
        assertEquals(200, status(get(base + "/earnings", adminToken())));
        assertEquals(403, status(get(base + "/earnings", createActivePartner("SEDAN", 4).ownerToken())));
        assertEquals(403, status(get(base + "/earnings", s.tourist())));
    }

    @Test
    void tripsReportListsBookingsWithStatusCounts() throws Exception {
        Scenario s = scenario();

        MvcResult report = get("/api/v1/travel-partners/" + s.partner().id() + "/reports/trips?to="
                + today().plusDays(10), s.partner().ownerToken());

        assertEquals(200, status(report));
        assertEquals(1, ((Number) json(report, "$.countsByStatus.COMPLETED")).intValue());
        assertEquals(1, ((Number) json(report, "$.countsByStatus.CANCELLED")).intValue());

        List<?> completedRows = json(report, "$.trips[?(@.bookingId=='" + s.completedBooking() + "')]");
        assertEquals(1, completedRows.size());
        assertEquals("Driver",
                ((List<?>) json(report, "$.trips[?(@.bookingId=='" + s.completedBooking() + "')].driver")).getFirst());
        assertEquals(s.partner().registrationNumber(),
                ((List<?>) json(report, "$.trips[?(@.bookingId=='" + s.completedBooking() + "')].vehicle")).getFirst());
    }

    @Test
    void vehicleAndDriverReportsShowUtilisationAndRatings() throws Exception {
        Scenario s = scenario();
        String base = "/api/v1/travel-partners/" + s.partner().id() + "/reports";
        String range = "?to=" + today().plusDays(10);

        MvcResult vehicles = get(base + "/vehicles" + range, s.partner().ownerToken());
        assertEquals(1, ((List<?>) json(vehicles, "$")).size());
        assertEquals(1, ((Number) json(vehicles, "$[0].completedTrips")).intValue());
        assertEquals(1, ((Number) json(vehicles, "$[0].cancelledTrips")).intValue());
        assertEquals(0, s.completedTotal().compareTo(decimal(vehicles, "$[0].revenue")));
        assertEquals(1, ((Number) json(vehicles, "$[0].daysOnRoad")).intValue());

        MvcResult drivers = get(base + "/drivers" + range, s.partner().ownerToken());
        assertEquals(s.driver().id().toString(), json(drivers, "$[0].driverId"));
        assertEquals(1, ((Number) json(drivers, "$[0].completedTrips")).intValue());
        assertEquals(5.0, ((Number) json(drivers, "$[0].averageRating")).doubleValue());
        assertEquals(1, ((Number) json(drivers, "$[0].reviewCount")).intValue());
    }

    @Test
    void cancellationsReportShowsWhoCancelledAndWhatWasRefunded() throws Exception {
        Scenario s = scenario();

        MvcResult report = get("/api/v1/travel-partners/" + s.partner().id() + "/reports/cancellations?to="
                + today().plusDays(10), s.partner().ownerToken());

        assertEquals(200, status(report));
        assertEquals(1, ((Number) json(report, "$.countsByCancelledBy.PARTNER_OWNER")).intValue());
        assertEquals("Breakdown", json(report, "$.cancellations[0].reason"));
        assertEquals("PARTNER_OWNER", json(report, "$.cancellations[0].cancelledBy"));

        BigDecimal refunded = decimal(report, "$.cancellations[0].refunded");
        assertTrue(refunded.signum() > 0);
        assertEquals(0, refunded.compareTo(decimal(report, "$.totalRefunded")));
    }

    @Test
    void reportDateRangesAreValidated() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String base = "/api/v1/travel-partners/" + partner.id() + "/reports/trips";

        assertEquals(400, status(get(base + "?from=2026-10-10&to=2026-10-01", partner.ownerToken())));
        assertEquals(400, status(get(base + "?from=2024-01-01&to=2026-10-01", partner.ownerToken())));
        assertEquals(400, status(get(base + "?from=not-a-date", partner.ownerToken())));
        assertEquals(200, status(get(base, partner.ownerToken())));
    }

    // ---- admin finance views -------------------------------------------------------------

    @Test
    void adminsSeePaymentsRefundsAndCancellations() throws Exception {
        Scenario s = scenario();
        String admin = adminToken();
        String window = "from=" + today().minusDays(1) + "&to=" + today();

        // Payments: both the kept and the refunded payment appear, with a correct summary.
        MvcResult payments = get("/api/v1/admin/payments?" + window + "&limit=500", admin);
        assertEquals(200, status(payments));

        List<?> refundedRows = json(payments, "$.payments[?(@.paymentId=='" + s.cancelledPayment() + "')].status");
        assertEquals("REFUNDED", refundedRows.getFirst());
        assertEquals(1, ((List<?>) json(payments, "$.payments[?(@.bookingId=='" + s.completedBooking() + "')]")).size());

        assertTrue(decimal(payments, "$.summary.collected").signum() > 0);
        assertTrue(decimal(payments, "$.summary.refunded").signum() > 0);
        assertEquals(0, decimal(payments, "$.summary.collected").subtract(decimal(payments, "$.summary.refunded"))
                .compareTo(decimal(payments, "$.summary.netCollected")));

        MvcResult onlyRefunded = get("/api/v1/admin/payments?" + window + "&status=REFUNDED&limit=500", admin);
        assertEquals(0, ((List<?>) json(onlyRefunded, "$.payments[?(@.status!='REFUNDED')]")).size());

        // Refunds.
        MvcResult refunds = get("/api/v1/admin/refunds?" + window + "&limit=500", admin);
        List<?> refundRows = json(refunds, "$.refunds[?(@.bookingId=='" + s.cancelledBooking() + "')]");
        assertEquals(1, refundRows.size());
        assertEquals("Breakdown", ((List<?>) json(refunds,
                "$.refunds[?(@.bookingId=='" + s.cancelledBooking() + "')].cancellationReason")).getFirst());
        assertTrue(decimal(refunds, "$.totalRefunded").signum() > 0);

        // Cancellations.
        MvcResult cancellations = get("/api/v1/admin/cancellations?" + window + "&limit=500", admin);
        assertEquals("PARTNER_OWNER", ((List<?>) json(cancellations,
                "$.cancellations[?(@.bookingId=='" + s.cancelledBooking() + "')].cancelledBy")).getFirst());
        assertTrue(((Number) json(cancellations, "$.countsByCancelledBy.PARTNER_OWNER")).intValue() >= 1);
    }

    @Test
    void financeViewsAreAdminOnlyAndValidateRanges() throws Exception {
        Partner partner = createActivePartner("SUV", 7);

        assertEquals(403, status(get("/api/v1/admin/payments", partner.ownerToken())));
        assertEquals(403, status(get("/api/v1/admin/refunds", touristToken())));
        assertEquals(401, status(get("/api/v1/admin/cancellations", null)));

        String admin = adminToken();
        assertEquals(200, status(get("/api/v1/admin/payments", admin)));
        assertEquals(400, status(get("/api/v1/admin/payments?from=2026-10-10&to=2026-10-01", admin)));
        assertEquals(400, status(get("/api/v1/admin/refunds?from=2020-01-01&to=2026-10-01", admin)));
        assertEquals(400, status(get("/api/v1/admin/payments?status=NOPE", admin)));
    }
}
