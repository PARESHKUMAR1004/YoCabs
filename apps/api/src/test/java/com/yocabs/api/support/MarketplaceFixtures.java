package com.yocabs.api.support;

import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Builds realistic marketplace state through the public REST API. */
public abstract class MarketplaceFixtures extends IntegrationTestBase {

    protected record Partner(
            UUID id,
            String ownerMobile,
            String ownerToken,
            UUID vehicleId,
            String registrationNumber
    ) {
    }

    protected record Driver(UUID id, String mobile, String token) {
    }

    /**
     * Registers a partner, has an admin activate it, gives it a Bhubaneswar
     * service area and one priced, available vehicle.
     */
    protected Partner createActivePartner(String category, int capacity) throws Exception {

        String adminToken = adminToken();
        String ownerMobile = randomMobile();

        MvcResult registration =
                post("/api/v1/auth/partner/register", null,
                        "{\"mobile\":\"" + ownerMobile + "\",\"ownerName\":\"Owner\","
                                + "\"businessName\":\"Test Travels " + UUID.randomUUID() + "\"}");
        UUID partnerId = UUID.fromString(json(registration, "$.travelPartnerId"));

        String ownerToken = otpLogin(ownerMobile);

        beforePartnerActivation(partnerId, ownerToken, adminToken);

        post("/api/v1/admin/travel-partners/" + partnerId + "/activate", adminToken, "{}");

        String registrationNumber = "OD02" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        MvcResult vehicle =
                post("/api/v1/travel-partners/" + partnerId + "/vehicles", ownerToken,
                        "{\"registrationNumber\":\"" + registrationNumber + "\",\"make\":\"Toyota\","
                                + "\"model\":\"Innova\",\"category\":\"" + category + "\","
                                + "\"passengerCapacity\":" + capacity + "}");
        UUID vehicleId = UUID.fromString(json(vehicle, "$.id"));

        post("/api/v1/travel-partners/" + partnerId + "/vehicles/" + vehicleId + "/make-available",
                ownerToken, "{}");

        /*
         * The service area belongs to the vehicle: without one it never
         * appears in a search.
         */
        post("/api/v1/travel-partners/" + partnerId + "/vehicles/" + vehicleId + "/service-areas",
                ownerToken,
                "{\"name\":\"Bhubaneswar\",\"latitude\":20.2961,\"longitude\":85.8245,\"radiusKm\":50}");

        post("/api/v1/pricing-configurations", ownerToken,
                "{\"vehicleId\":\"" + vehicleId + "\",\"tripType\":\"CHAUFFEUR_ONE_WAY\","
                        + "\"baseFee\":600,\"perKmCharge\":20,\"driverAllowance\":350,"
                        + "\"minimumBillableKm\":50}");

        return new Partner(partnerId, ownerMobile, ownerToken, vehicleId, registrationNumber);
    }

    protected static final byte[] PNG_BYTES = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
    );

    protected static final byte[] PDF_BYTES =
            "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    protected MvcResult uploadFileRaw(
            String token, String ownerType, UUID ownerId, String documentType,
            String filename, String contentType, byte[] content
    ) throws Exception {
        return perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/v1/documents")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", filename, contentType, content))
                        .param("ownerType", ownerType)
                        .param("ownerId", ownerId.toString())
                        .param("documentType", documentType)
                        .header("Authorization", "Bearer " + token));
    }

    protected UUID uploadFile(
            String token, String ownerType, UUID ownerId, String documentType,
            String filename, String contentType, byte[] content
    ) throws Exception {
        MvcResult result = uploadFileRaw(token, ownerType, ownerId, documentType, filename, contentType, content);
        org.junit.jupiter.api.Assertions.assertEquals(201, status(result), body(result));
        return UUID.fromString(json(result, "$.id"));
    }

    /** Hook for suites that configure a verification gate (e.g. required documents). */
    protected void beforePartnerActivation(UUID partnerId, String ownerToken, String adminToken)
            throws Exception {
    }

    /** Drives a confirmed booking through assignment, start and completion. */
    protected void completeTrip(Partner partner, Driver driver, String tourist, UUID bookingId)
            throws Exception {
        post("/api/v1/bookings/" + bookingId + "/driver", partner.ownerToken(),
                "{\"driverId\":\"" + driver.id() + "\"}");
        post("/api/v1/bookings/" + bookingId + "/start", driver.token(), tripCode(tourist, bookingId));
        post("/api/v1/bookings/" + bookingId + "/complete", driver.token(), "{}");
    }

    /** The request body a driver sends: the code the tourist can currently read in their app. */
    protected String tripCode(String tourist, UUID bookingId) throws Exception {
        return "{\"code\":\"" + json(get("/api/v1/bookings/" + bookingId, tourist), "$.tripCode") + "\"}";
    }

    protected Driver addDriver(Partner partner) throws Exception {

        String mobile = randomMobile();

        MvcResult result =
                post("/api/v1/travel-partners/" + partner.id() + "/drivers", partner.ownerToken(),
                        "{\"name\":\"Driver\",\"mobile\":\"" + mobile + "\","
                                + "\"licenseNumber\":\"DL" + UUID.randomUUID().toString().substring(0, 10) + "\"}");

        return new Driver(UUID.fromString(json(result, "$.id")), mobile, otpLogin(mobile));
    }

    /** A submitted trip request from Bhubaneswar Airport to Puri on the given dates. */
    protected UUID submittedTripRequest(String touristToken, LocalDate start, LocalDate end, int passengers)
            throws Exception {

        MvcResult created =
                post("/api/v1/trip-requests", touristToken,
                        "{\"pickupDescription\":\"Bhubaneswar Airport\",\"pickupLatitude\":20.2444,"
                                + "\"pickupLongitude\":85.8178,\"destinationDescription\":\"Puri\","
                                + "\"destinationLatitude\":19.8135,\"destinationLongitude\":85.8312,"
                                + "\"startDate\":\"" + start + "\",\"endDate\":\"" + end + "\","
                                + "\"passengerCount\":" + passengers + ","
                                + "\"tripBrief\":\"Airport to Puri\"}");

        UUID tripRequestId = UUID.fromString(json(created, "$"));

        post("/api/v1/trip-requests/" + tripRequestId + "/submit", touristToken, "{}");

        return tripRequestId;
    }

    protected UUID book(String touristToken, UUID tripRequestId, UUID vehicleId, UUID negotiationId)
            throws Exception {

        MvcResult result = bookRaw(touristToken, tripRequestId, vehicleId, negotiationId, idempotencyKey());

        return UUID.fromString(json(result, "$.id"));
    }

    protected MvcResult bookRaw(
            String touristToken,
            UUID tripRequestId,
            UUID vehicleId,
            UUID negotiationId,
            String key
    ) throws Exception {

        String negotiation = negotiationId == null ? "" : ",\"negotiationId\":\"" + negotiationId + "\"";

        return postWithHeader(
                "/api/v1/bookings", touristToken,
                "{\"tripRequestId\":\"" + tripRequestId + "\",\"vehicleId\":\"" + vehicleId + "\","
                        + "\"tripType\":\"CHAUFFEUR_ONE_WAY\"" + negotiation + "}",
                "Idempotency-Key", key
        );
    }

    /** Pays the booking's token through the sandbox gateway and returns the payment id. */
    protected UUID pay(String touristToken, UUID bookingId) throws Exception {

        MvcResult payment = post("/api/v1/bookings/" + bookingId + "/payments", touristToken, "{}");
        UUID paymentId = UUID.fromString(json(payment, "$.id"));

        post("/api/v1/dev/payments/" + paymentId + "/simulate", touristToken, "{\"outcome\":\"SUCCESS\"}");

        return paymentId;
    }

    protected BigDecimal decimal(MvcResult result, String path) throws Exception {
        Object value = json(result, path);
        return new BigDecimal(String.valueOf(value));
    }
}
