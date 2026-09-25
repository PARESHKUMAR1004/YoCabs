package com.yocabs.api;

import com.yocabs.api.support.MarketplaceFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportIntegrationTest extends MarketplaceFixtures {

    private UUID open(String token, String category, String subject, UUID bookingId) throws Exception {
        String booking = bookingId == null ? "" : ",\"bookingId\":\"" + bookingId + "\"";
        MvcResult result =
                post("/api/v1/support/tickets", token,
                        "{\"category\":\"" + category + "\",\"subject\":\"" + subject + "\","
                                + "\"description\":\"Something went wrong\"" + booking + "}");
        assertEquals(201, status(result), body(result));
        return UUID.fromString(json(result, "$.ticket.id"));
    }

    private boolean hasNotification(String token, String type) throws Exception {
        return body(get("/api/v1/notifications", token)).contains(type);
    }

    @Test
    void aTicketIsWorkedFromOpenToClosedWithARunningConversation() throws Exception {
        String admin = adminToken();
        String tourist = touristToken();

        UUID ticketId = open(tourist, "PAYMENT_ISSUE", "Charged twice", null);

        MvcResult created = get("/api/v1/support/tickets/" + ticketId, tourist);
        assertEquals("OPEN", json(created, "$.ticket.status"));
        assertEquals(1, ((List<?>) json(created, "$.messages")).size());
        assertEquals("TOURIST", json(created, "$.messages[0].authorRole"));

        // It shows in the admin queue and on the dashboard; admins are told about new tickets.
        assertTrue(body(get("/api/v1/admin/support/tickets?status=OPEN&limit=200", admin)).contains(ticketId.toString()));
        assertTrue(((Number) json(get("/api/v1/admin/dashboard", admin), "$.openSupportTickets")).intValue() >= 1);
        assertTrue(hasNotification(admin, "SUPPORT_TICKET_OPENED"));

        // An admin's reply auto-assigns the ticket and notifies the requester.
        MvcResult replied = post("/api/v1/support/tickets/" + ticketId + "/messages", admin,
                "{\"body\":\"We are looking into it.\"}");
        assertEquals(200, status(replied));
        assertEquals("IN_PROGRESS", json(replied, "$.ticket.status"));
        assertEquals(2, ((List<?>) json(replied, "$.messages")).size());
        assertTrue(hasNotification(tourist, "SUPPORT_REPLY"));

        // The requester answers; the assigned admin is told.
        assertEquals(200, status(post("/api/v1/support/tickets/" + ticketId + "/messages", tourist,
                "{\"body\":\"Thanks, any update?\"}")));
        assertTrue(hasNotification(admin, "SUPPORT_MESSAGE"));

        // Resolution notifies the requester; replying afterwards reopens the ticket.
        assertEquals("RESOLVED", json(post("/api/v1/admin/support/tickets/" + ticketId + "/resolve", admin, "{}"),
                "$.status"));
        assertTrue(hasNotification(tourist, "SUPPORT_RESOLVED"));

        MvcResult reopened = post("/api/v1/support/tickets/" + ticketId + "/messages", tourist,
                "{\"body\":\"Still not fixed\"}");
        assertEquals("IN_PROGRESS", json(reopened, "$.ticket.status"));
        assertEquals(4, ((List<?>) json(reopened, "$.messages")).size());

        // A closed ticket accepts nothing more.
        assertEquals("CLOSED", json(post("/api/v1/admin/support/tickets/" + ticketId + "/close", admin, "{}"),
                "$.status"));
        assertEquals(409, status(post("/api/v1/support/tickets/" + ticketId + "/messages", tourist,
                "{\"body\":\"hello?\"}")));
        assertEquals(409, status(post("/api/v1/admin/support/tickets/" + ticketId + "/resolve", admin, "{}")));

        String audit = body(get("/api/v1/admin/audit-logs?limit=500", admin));
        assertTrue(audit.contains("SUPPORT_TICKET_RESOLVED"));
        assertTrue(audit.contains("SUPPORT_TICKET_CLOSED"));
    }

    @Test
    void ticketsArePrivateToTheirCreatorAndAdmins() throws Exception {
        String owner = touristToken();
        String stranger = touristToken();
        UUID ticketId = open(owner, "OTHER", "Private matter", null);

        assertEquals(403, status(get("/api/v1/support/tickets/" + ticketId, stranger)));
        assertEquals(403, status(post("/api/v1/support/tickets/" + ticketId + "/messages", stranger,
                "{\"body\":\"hi\"}")));

        assertEquals(200, status(get("/api/v1/support/tickets/" + ticketId, owner)));
        assertEquals(200, status(get("/api/v1/support/tickets/" + ticketId, adminToken())));

        assertTrue(body(get("/api/v1/support/tickets", owner)).contains(ticketId.toString()));
        assertTrue(!body(get("/api/v1/support/tickets", stranger)).contains(ticketId.toString()));

        // Only admins work the queue.
        assertEquals(403, status(get("/api/v1/admin/support/tickets", owner)));
        assertEquals(403, status(post("/api/v1/admin/support/tickets/" + ticketId + "/assign", owner, "{}")));
        assertEquals(401, status(get("/api/v1/support/tickets", null)));
        assertEquals(404, status(get("/api/v1/support/tickets/" + UUID.randomUUID(), owner)));
    }

    @Test
    void ticketsCanReferenceOnlyTheCallersOwnBookings() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String tourist = touristToken();
        String stranger = touristToken();

        UUID trip = submittedTripRequest(tourist, today().plusDays(80), today().plusDays(80), 3);
        UUID booking = book(tourist, trip, partner.vehicleId(), null);

        MvcResult linked = get("/api/v1/support/tickets/" + open(tourist, "BOOKING_ISSUE", "Wrong pickup", booking),
                tourist);
        assertEquals(booking.toString(), json(linked, "$.ticket.bookingId"));

        assertEquals(403, status(post("/api/v1/support/tickets", stranger,
                "{\"category\":\"BOOKING_ISSUE\",\"subject\":\"Not mine\",\"description\":\"x\","
                        + "\"bookingId\":\"" + booking + "\"}")));
        assertEquals(404, status(post("/api/v1/support/tickets", tourist,
                "{\"category\":\"BOOKING_ISSUE\",\"subject\":\"Ghost\",\"description\":\"x\","
                        + "\"bookingId\":\"" + UUID.randomUUID() + "\"}")));
    }

    @Test
    void partnersAndDriversCanRaiseTicketsButAdminsCannot() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        Driver driver = addDriver(partner);

        UUID partnerTicket = open(partner.ownerToken(), "PAYMENT_ISSUE", "Payout delayed", null);
        UUID driverTicket = open(driver.token(), "SAFETY", "Unsafe pickup point", null);

        String queue = body(get("/api/v1/admin/support/tickets?limit=200", adminToken()));
        assertTrue(queue.contains(partnerTicket.toString()));
        assertTrue(queue.contains(driverTicket.toString()));
        assertEquals("PARTNER_OWNER", json(get("/api/v1/support/tickets/" + partnerTicket, adminToken()),
                "$.ticket.creatorRole"));
        assertEquals("DRIVER", json(get("/api/v1/support/tickets/" + driverTicket, adminToken()),
                "$.ticket.creatorRole"));

        assertEquals(403, status(post("/api/v1/support/tickets", adminToken(),
                "{\"category\":\"OTHER\",\"subject\":\"x\",\"description\":\"y\"}")));
    }

    @Test
    void ticketInputIsValidated() throws Exception {
        String tourist = touristToken();

        assertEquals(400, status(post("/api/v1/support/tickets", tourist,
                "{\"category\":\"OTHER\",\"subject\":\"\",\"description\":\"y\"}")));
        assertEquals(400, status(post("/api/v1/support/tickets", tourist,
                "{\"category\":\"OTHER\",\"subject\":\"s\",\"description\":\"\"}")));
        assertEquals(400, status(post("/api/v1/support/tickets", tourist,
                "{\"category\":\"NOT_A_CATEGORY\",\"subject\":\"s\",\"description\":\"y\"}")));
        assertEquals(400, status(post("/api/v1/support/tickets", tourist,
                "{\"subject\":\"s\",\"description\":\"y\"}")));

        UUID ticketId = open(tourist, "OTHER", "ok", null);
        assertEquals(400, status(post("/api/v1/support/tickets/" + ticketId + "/messages", tourist, "{\"body\":\" \"}")));
        assertEquals(400, status(post("/api/v1/support/tickets/" + ticketId + "/messages", tourist,
                "{\"body\":\"" + "x".repeat(4001) + "\"}")));
    }
}
