package com.yocabs.api;

import com.yocabs.api.support.MarketplaceFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs with a verification gate and a 2% commission so settlement produces a payable surplus. */
@TestPropertySource(properties = {
        "yocabs.verification.required-partner-documents=TRADE_LICENSE",
        "yocabs.booking.commission-percentage=2"
})
class AdminOperationsIntegrationTest extends MarketplaceFixtures {

    private static final byte[] PDF = "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes(StandardCharsets.US_ASCII);

    @Override
    protected void beforePartnerActivation(UUID partnerId, String ownerToken, String adminToken) throws Exception {
        UUID documentId = upload(ownerToken, "PARTNER", partnerId, "TRADE_LICENSE", "license.pdf", "application/pdf", PDF);
        assertEquals(200, status(post("/api/v1/admin/documents/" + documentId + "/approve", adminToken, "{}")));
    }

    private UUID upload(
            String token, String ownerType, UUID ownerId, String type,
            String filename, String contentType, byte[] content
    ) throws Exception {
        MvcResult result = uploadRaw(token, ownerType, ownerId, type, filename, contentType, content);
        assertEquals(201, status(result), body(result));
        return UUID.fromString(json(result, "$.id"));
    }

    private MvcResult uploadRaw(
            String token, String ownerType, UUID ownerId, String type,
            String filename, String contentType, byte[] content
    ) throws Exception {
        return perform(
                MockMvcRequestBuilders.multipart("/api/v1/documents")
                        .file(new MockMultipartFile("file", filename, contentType, content))
                        .param("ownerType", ownerType)
                        .param("ownerId", ownerId.toString())
                        .param("documentType", type)
                        .header("Authorization", "Bearer " + token));
    }

    @Test
    void aPartnerCannotGoLiveUntilItsRequiredDocumentsAreApproved() throws Exception {
        String admin = adminToken();
        String mobile = randomMobile();

        UUID partnerId = UUID.fromString(json(
                post("/api/v1/auth/partner/register", null,
                        "{\"mobile\":\"" + mobile + "\",\"ownerName\":\"Owner\",\"businessName\":\"Gate " + UUID.randomUUID() + "\"}"),
                "$.travelPartnerId"));
        String owner = otpLogin(mobile);

        // Missing documents.
        MvcResult blocked = post("/api/v1/admin/travel-partners/" + partnerId + "/activate", admin, "{}");
        assertEquals(409, status(blocked));
        assertTrue(body(blocked).contains("TRADE_LICENSE"));

        // Pending is not enough ...
        UUID documentId = upload(owner, "PARTNER", partnerId, "TRADE_LICENSE", "tl.pdf", "application/pdf", PDF);
        assertEquals(409, status(post("/api/v1/admin/travel-partners/" + partnerId + "/activate", admin, "{}")));

        // ... a rejected document is not enough either.
        assertEquals(400, status(post("/api/v1/admin/documents/" + documentId + "/reject", admin, "{}")));
        assertEquals(200, status(post("/api/v1/admin/documents/" + documentId + "/reject", admin,
                "{\"reason\":\"Illegible scan\"}")));
        assertTrue(body(get("/api/v1/notifications", owner)).contains("DOCUMENT_REJECTED"));
        assertEquals(409, status(post("/api/v1/admin/travel-partners/" + partnerId + "/activate", admin, "{}")));

        UUID resubmitted = upload(owner, "PARTNER", partnerId, "TRADE_LICENSE", "tl2.pdf", "application/pdf", PDF);
        assertEquals(200, status(post("/api/v1/admin/documents/" + resubmitted + "/approve", admin, "{}")));
        assertEquals(409, status(post("/api/v1/admin/documents/" + resubmitted + "/approve", admin, "{}")));

        MvcResult activated = post("/api/v1/admin/travel-partners/" + partnerId + "/activate", admin, "{}");
        assertEquals(200, status(activated));
        assertEquals("ACTIVE", json(activated, "$.status"));

        String audit = body(get("/api/v1/admin/audit-logs", admin));
        assertTrue(audit.contains("PARTNER_ACTIVATED"));
        assertTrue(audit.contains("DOCUMENT_REJECTED"));
    }

    @Test
    void documentUploadsAreValidatedAndPrivate() throws Exception {
        Partner partner = createActivePartner("SEDAN", 4);
        Partner other = createActivePartner("SEDAN", 4);

        // Wrong content: a script mislabelled as a PDF is refused.
        assertEquals(400, status(uploadRaw(partner.ownerToken(), "PARTNER", partner.id(), "GST_CERTIFICATE",
                "evil.pdf", "application/pdf", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8))));
        // Unsupported type.
        assertEquals(400, status(uploadRaw(partner.ownerToken(), "PARTNER", partner.id(), "GST_CERTIFICATE",
                "notes.txt", "text/plain", PDF)));
        // Someone else's organisation.
        assertEquals(403, status(uploadRaw(other.ownerToken(), "PARTNER", partner.id(), "GST_CERTIFICATE",
                "x.pdf", "application/pdf", PDF)));
        // Someone else's vehicle.
        assertEquals(403, status(uploadRaw(other.ownerToken(), "VEHICLE", partner.vehicleId(), "RC_BOOK",
                "rc.pdf", "application/pdf", PDF)));

        UUID vehicleDocument =
                upload(partner.ownerToken(), "VEHICLE", partner.vehicleId(), "RC_BOOK", "rc.pdf", "application/pdf", PDF);

        MvcResult download = get("/api/v1/documents/" + vehicleDocument + "/content", partner.ownerToken());
        assertEquals(200, status(download));
        assertEquals("nosniff", download.getResponse().getHeader("X-Content-Type-Options"));
        assertTrue(download.getResponse().getHeader("Content-Disposition").startsWith("attachment"));
        assertEquals(PDF.length, download.getResponse().getContentAsByteArray().length);

        assertEquals(403, status(get("/api/v1/documents/" + vehicleDocument + "/content", other.ownerToken())));
        assertEquals(403, status(get("/api/v1/documents/" + vehicleDocument + "/content", touristToken())));
        assertEquals(200, status(get("/api/v1/documents/" + vehicleDocument + "/content", adminToken())));
    }

    @Test
    void completedTripsSettleIntoAPayableWalletAndPayoutsAreControlled() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        Driver driver = addDriver(partner);
        String tourist = touristToken();
        String admin = adminToken();

        UUID trip = submittedTripRequest(tourist, today(), today(), 4);
        UUID booking = book(tourist, trip, partner.vehicleId(), null);
        BigDecimal total = decimal(get("/api/v1/bookings/" + booking, tourist), "$.totalAmount");
        pay(tourist, booking);
        completeTrip(partner, driver, tourist, booking);

        // Token 25% - commission 2% = 23% of the fare is owed to the partner.
        BigDecimal token = total.multiply(new BigDecimal("0.25")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal commission = total.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal surplus = token.subtract(commission);

        MvcResult wallet = get("/api/v1/travel-partners/" + partner.id() + "/wallet", partner.ownerToken());
        assertEquals(0, surplus.compareTo(decimal(wallet, "$.balance")));
        assertEquals("CREDIT", json(wallet, "$.entries[0].type"));

        // Cannot withdraw more than is available.
        assertEquals(409, status(post("/api/v1/travel-partners/" + partner.id() + "/payouts", partner.ownerToken(),
                "{\"amount\":" + surplus.add(BigDecimal.ONE) + "}")));

        BigDecimal first = surplus.divide(new BigDecimal("2"), 2, RoundingMode.DOWN);
        MvcResult requested = post("/api/v1/travel-partners/" + partner.id() + "/payouts", partner.ownerToken(),
                "{\"amount\":" + first + "}");
        assertEquals(201, status(requested));
        UUID payoutId = UUID.fromString(json(requested, "$.id"));

        // Pending requests reduce what can still be requested.
        assertEquals(409, status(post("/api/v1/travel-partners/" + partner.id() + "/payouts", partner.ownerToken(),
                "{\"amount\":" + surplus + "}")));

        assertTrue(body(get("/api/v1/admin/payouts", admin)).contains(payoutId.toString()));
        assertEquals(403, status(get("/api/v1/admin/payouts", partner.ownerToken())));

        // A bank reference is mandatory to mark a payout paid.
        assertEquals(400, status(post("/api/v1/admin/payouts/" + payoutId + "/pay", admin, "{}")));

        MvcResult paid = post("/api/v1/admin/payouts/" + payoutId + "/pay", admin, "{\"bankReference\":\"UTR123\"}");
        assertEquals("PAID", json(paid, "$.status"));
        assertEquals(409, status(post("/api/v1/admin/payouts/" + payoutId + "/pay", admin, "{\"bankReference\":\"UTR124\"}")));

        MvcResult after = get("/api/v1/travel-partners/" + partner.id() + "/wallet", partner.ownerToken());
        assertEquals(0, surplus.subtract(first).compareTo(decimal(after, "$.balance")));
        assertEquals("DEBIT", json(after, "$.entries[0].type"));
        assertTrue(body(get("/api/v1/notifications", partner.ownerToken())).contains("PAYOUT_PAID"));

        // A rejected payout leaves the balance untouched.
        BigDecimal second = surplus.subtract(first).divide(new BigDecimal("2"), 2, RoundingMode.DOWN);
        UUID rejected = UUID.fromString(json(
                post("/api/v1/travel-partners/" + partner.id() + "/payouts", partner.ownerToken(),
                        "{\"amount\":" + second + "}"), "$.id"));
        assertEquals("REJECTED", json(
                post("/api/v1/admin/payouts/" + rejected + "/reject", admin, "{\"note\":\"Bank details missing\"}"),
                "$.status"));
        assertEquals(0, surplus.subtract(first)
                .compareTo(decimal(get("/api/v1/travel-partners/" + partner.id() + "/wallet", partner.ownerToken()), "$.balance")));

        assertTrue(body(get("/api/v1/admin/audit-logs", admin)).contains("PAYOUT_PAID"));
    }

    @Test
    void notificationsAreReadableAndScopedToTheirRecipient() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String tourist = touristToken();
        String stranger = touristToken();

        UUID trip = submittedTripRequest(tourist, today().plusDays(60), today().plusDays(60), 3);
        UUID booking = book(tourist, trip, partner.vehicleId(), null);
        pay(tourist, booking);

        MvcResult list = get("/api/v1/notifications?unreadOnly=true", tourist);
        int unread = json(get("/api/v1/notifications/unread-count", tourist), "$.unread");
        assertTrue(unread >= 1);

        String notificationId = ((java.util.List<?>) json(list, "$[?(@.type=='BOOKING_CONFIRMED')].id")).getFirst().toString();

        assertEquals(404, status(post("/api/v1/notifications/" + notificationId + "/read", stranger, "{}")));
        assertEquals(204, status(post("/api/v1/notifications/" + notificationId + "/read", tourist, "{}")));

        int afterOne = json(get("/api/v1/notifications/unread-count", tourist), "$.unread");
        assertEquals(unread - 1, afterOne);

        assertEquals(204, status(post("/api/v1/notifications/read-all", tourist, "{}")));
        assertEquals(0, (int) json(get("/api/v1/notifications/unread-count", tourist), "$.unread"));
    }

    @Test
    void anAdminCanCreateAPartnerAndGiveItASingleOwnerLogin() throws Exception {
        String admin = adminToken();

        UUID partnerId = UUID.fromString(json(
                post("/api/v1/travel-partners", admin, "{\"name\":\"Admin Made " + UUID.randomUUID() + "\"}"), "$"));

        String mobile = randomMobile();
        MvcResult owner = post("/api/v1/admin/travel-partners/" + partnerId + "/owner", admin,
                "{\"name\":\"Amit\",\"mobile\":\"" + mobile + "\"}");
        assertEquals(201, status(owner));
        assertEquals("PARTNER_OWNER", json(owner, "$.role"));

        // One owner per partner, and mobile numbers are unique.
        assertEquals(409, status(post("/api/v1/admin/travel-partners/" + partnerId + "/owner", admin,
                "{\"name\":\"Second\",\"mobile\":\"" + randomMobile() + "\"}")));

        String ownerToken = otpLogin(mobile);
        assertEquals(partnerId.toString(), json(get("/api/v1/auth/me", ownerToken), "$.partnerId"));

        // Only admins may create partners.
        assertEquals(403, status(post("/api/v1/travel-partners", touristToken(), "{\"name\":\"Nope\"}")));
    }

    @Test
    void adminsCanBlockUsersAndBlockedUsersAreLockedOut() throws Exception {
        String admin = adminToken();
        String mobile = randomMobile();
        String tourist = otpLogin(mobile);
        UUID touristId = UUID.fromString(json(get("/api/v1/auth/me", tourist), "$.userId"));

        assertEquals(200, status(post("/api/v1/admin/users/" + touristId + "/block", admin, "{}")));

        post("/api/v1/auth/otp/request", null, "{\"mobile\":\"" + mobile + "\"}");
        assertEquals(403, status(post("/api/v1/auth/otp/verify", null,
                "{\"mobile\":\"" + mobile + "\",\"code\":\"" + otpSender.lastCode("+91" + mobile) + "\"}")));

        assertEquals(200, status(post("/api/v1/admin/users/" + touristId + "/unblock", admin, "{}")));
    }
}
