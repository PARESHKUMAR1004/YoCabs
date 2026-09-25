package com.yocabs.api;

import com.yocabs.api.support.MarketplaceFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SecurityIntegrationTest extends MarketplaceFixtures {

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        assertEquals(401, status(get("/api/v1/bookings", null)));
        assertEquals(401, status(get("/api/v1/bookings", "not-a-real-token")));
        assertEquals(401, status(post("/api/v1/trip-requests", null, "{}")));
    }

    @Test
    void publicEndpointsStayOpen() throws Exception {
        assertEquals(200, status(get("/actuator/health", null)));
        assertEquals(400, status(post("/api/v1/trip-search", null, "{}")));
    }

    @Test
    void touristsCannotUseAdminOrPartnerEndpoints() throws Exception {
        String tourist = touristToken();

        assertEquals(403, status(get("/api/v1/admin/dashboard", tourist)));
        assertEquals(403, status(get("/api/v1/admin/users?role=TOURIST", tourist)));
        assertEquals(403, status(get("/api/v1/admin/documents", tourist)));
    }

    @Test
    void adminCanViewTheDashboard() throws Exception {
        MvcResult dashboard = get("/api/v1/admin/dashboard", adminToken());

        assertEquals(200, status(dashboard));
        assertEquals(true, body(dashboard).contains("partnersByStatus"));
    }

    @Test
    void wrongOtpIsRejectedAndCodeIsBurnedAfterTooManyAttempts() throws Exception {
        String mobile = randomMobile();

        post("/api/v1/auth/otp/request", null, "{\"mobile\":\"" + mobile + "\"}");
        String correct = otpSender.lastCode("+91" + mobile);
        String wrong = correct.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            assertEquals(401,
                    status(post("/api/v1/auth/otp/verify", null,
                            "{\"mobile\":\"" + mobile + "\",\"code\":\"" + wrong + "\"}")));
        }

        // Even the correct code no longer works: the challenge is exhausted.
        assertEquals(401,
                status(post("/api/v1/auth/otp/verify", null,
                        "{\"mobile\":\"" + mobile + "\",\"code\":\"" + correct + "\"}")));
    }

    @Test
    void anOtpCanOnlyBeUsedOnce() throws Exception {
        String mobile = randomMobile();

        post("/api/v1/auth/otp/request", null, "{\"mobile\":\"" + mobile + "\"}");
        String code = otpSender.lastCode("+91" + mobile);
        String body = "{\"mobile\":\"" + mobile + "\",\"code\":\"" + code + "\"}";

        assertEquals(200, status(post("/api/v1/auth/otp/verify", null, body)));
        assertEquals(401, status(post("/api/v1/auth/otp/verify", null, body)));
    }

    @Test
    void refreshTokensRotateAndReuseRevokesTheFamily() throws Exception {
        String mobile = randomMobile();

        post("/api/v1/auth/otp/request", null, "{\"mobile\":\"" + mobile + "\"}");
        MvcResult login =
                post("/api/v1/auth/otp/verify", null,
                        "{\"mobile\":\"" + mobile + "\",\"code\":\"" + otpSender.lastCode("+91" + mobile) + "\"}");
        String firstRefresh = json(login, "$.refreshToken");

        MvcResult rotated = post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + firstRefresh + "\"}");
        assertEquals(200, status(rotated));
        String secondRefresh = json(rotated, "$.refreshToken");
        assertNotEquals(firstRefresh, secondRefresh);

        // Presenting the already-rotated token is treated as theft ...
        assertEquals(401, status(post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + firstRefresh + "\"}")));
        // ... and invalidates the descendant too.
        assertEquals(401, status(post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + secondRefresh + "\"}")));
    }

    @Test
    void logoutInvalidatesTheRefreshToken() throws Exception {
        String mobile = randomMobile();

        post("/api/v1/auth/otp/request", null, "{\"mobile\":\"" + mobile + "\"}");
        MvcResult login =
                post("/api/v1/auth/otp/verify", null,
                        "{\"mobile\":\"" + mobile + "\",\"code\":\"" + otpSender.lastCode("+91" + mobile) + "\"}");
        String refresh = json(login, "$.refreshToken");

        assertEquals(204, status(post("/api/v1/auth/logout", null, "{\"refreshToken\":\"" + refresh + "\"}")));
        assertEquals(401, status(post("/api/v1/auth/refresh", null, "{\"refreshToken\":\"" + refresh + "\"}")));
    }

    @Test
    void adminAccountLocksAfterRepeatedFailedLogins() throws Exception {
        String root = adminToken();
        String email = "lock-" + UUID.randomUUID() + "@yocabs.test";
        String password = "Another-Str0ng-Passw0rd";

        assertEquals(201,
                status(post("/api/v1/admin/admins", root,
                        "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"displayName\":\"Ops\"}")));

        for (int i = 0; i < 5; i++) {
            assertEquals(401,
                    status(post("/api/v1/auth/admin/login", null,
                            "{\"email\":\"" + email + "\",\"password\":\"wrong-password-" + i + "\"}")));
        }

        assertEquals(401,
                status(post("/api/v1/auth/admin/login", null,
                        "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")));
    }

    @Test
    void onlyASuperAdminCanCreateAdmins() throws Exception {
        String root = adminToken();
        String email = "ops-" + UUID.randomUUID() + "@yocabs.test";
        String password = "Another-Str0ng-Passw0rd";

        post("/api/v1/admin/admins", root,
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"displayName\":\"Ops\"}");

        String ordinaryAdmin =
                json(post("/api/v1/auth/admin/login", null,
                        "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"), "$.accessToken");

        assertEquals(403,
                status(post("/api/v1/admin/admins", ordinaryAdmin,
                        "{\"email\":\"x-" + UUID.randomUUID() + "@yocabs.test\","
                                + "\"password\":\"Another-Str0ng-Passw0rd\",\"displayName\":\"X\"}")));
    }

    @Test
    void partnersCannotTouchEachOthersData() throws Exception {
        Partner a = createActivePartner("SUV", 7);
        Partner b = createActivePartner("SEDAN", 4);

        // Vehicles.
        assertEquals(403, status(get("/api/v1/travel-partners/" + a.id() + "/vehicles", b.ownerToken())));
        assertEquals(200, status(get("/api/v1/travel-partners/" + a.id() + "/vehicles", a.ownerToken())));

        // Pricing: a supplied vehicle id is not proof of ownership.
        assertEquals(403, status(get("/api/v1/pricing-configurations/vehicle/" + a.vehicleId(), b.ownerToken())));
        assertEquals(403,
                status(post("/api/v1/pricing-configurations", b.ownerToken(),
                        "{\"vehicleId\":\"" + a.vehicleId() + "\",\"tripType\":\"CHAUFFEUR_ROUND_TRIP\","
                                + "\"baseFee\":1,\"perKmCharge\":1,\"driverAllowance\":1,\"minimumBillableKm\":1}")));

        // Service areas, drivers, wallet, staff.
        assertEquals(403,
                status(post("/api/v1/travel-partners/" + a.id() + "/vehicles/" + a.vehicleId() + "/service-areas", b.ownerToken(),
                        "{\"name\":\"X\",\"latitude\":20,\"longitude\":85,\"radiusKm\":5}")));
        assertEquals(403, status(get("/api/v1/travel-partners/" + a.id() + "/drivers", b.ownerToken())));
        assertEquals(403, status(get("/api/v1/travel-partners/" + a.id() + "/wallet", b.ownerToken())));
        assertEquals(403,
                status(post("/api/v1/travel-partners/" + a.id() + "/staff", b.ownerToken(),
                        "{\"name\":\"S\",\"mobile\":\"" + randomMobile() + "\"}")));
    }

    @Test
    void touristsCannotReadEachOthersBookingsOrTripRequests() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String owner = touristToken();
        String stranger = touristToken();

        UUID tripRequestId = submittedTripRequest(owner, today().plusDays(30), today().plusDays(30), 3);
        UUID bookingId = book(owner, tripRequestId, partner.vehicleId(), null);

        assertEquals(200, status(get("/api/v1/bookings/" + bookingId, owner)));
        assertEquals(403, status(get("/api/v1/bookings/" + bookingId, stranger)));
        assertEquals(403, status(get("/api/v1/trip-requests/" + tripRequestId, stranger)));
        assertEquals(403, status(post("/api/v1/bookings/" + bookingId + "/cancel", stranger, "{}")));
        assertEquals(403, status(post("/api/v1/bookings/" + bookingId + "/payments", stranger, "{}")));
    }

    @Test
    void staffHaveOrganisationAccessButCannotManageStaff() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        String staffMobile = randomMobile();

        assertEquals(201,
                status(post("/api/v1/travel-partners/" + partner.id() + "/staff", partner.ownerToken(),
                        "{\"name\":\"Clerk\",\"mobile\":\"" + staffMobile + "\"}")));

        String staffToken = otpLogin(staffMobile);

        assertEquals(200, status(get("/api/v1/travel-partners/" + partner.id() + "/vehicles", staffToken)));
        assertEquals(403,
                status(post("/api/v1/travel-partners/" + partner.id() + "/staff", staffToken,
                        "{\"name\":\"Other\",\"mobile\":\"" + randomMobile() + "\"}")));
        assertEquals(403,
                status(post("/api/v1/travel-partners/" + partner.id() + "/payouts", staffToken,
                        "{\"amount\":1}")));
    }

    @Test
    void aDeactivatedDriverCanNoLongerSignIn() throws Exception {
        Partner partner = createActivePartner("SUV", 7);
        Driver driver = addDriver(partner);

        assertEquals(200,
                status(post("/api/v1/travel-partners/" + partner.id() + "/drivers/" + driver.id() + "/deactivate",
                        partner.ownerToken(), "{}")));

        post("/api/v1/auth/otp/request", null, "{\"mobile\":\"" + driver.mobile() + "\"}");
        assertEquals(403,
                status(post("/api/v1/auth/otp/verify", null,
                        "{\"mobile\":\"" + driver.mobile() + "\",\"code\":\""
                                + otpSender.lastCode("+91" + driver.mobile()) + "\"}")));
    }

    @Test
    void errorResponsesCarryACorrelationId() throws Exception {
        MvcResult result = get("/api/v1/bookings", null);

        assertEquals(401, status(result));
        assertEquals(true, result.getResponse().getHeader("X-Correlation-Id") != null);
        assertEquals(true, body(result).contains("correlationId"));
    }
}
