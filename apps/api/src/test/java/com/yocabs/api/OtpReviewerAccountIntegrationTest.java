package com.yocabs.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The reviewer account is configured in IntegrationTestBase
 * (yocabs.otp.reviewer-mobile=9000000001, reviewer-code=123456).
 */
class OtpReviewerAccountIntegrationTest extends com.yocabs.api.support.MarketplaceFixtures {

    @Test
    void theReviewerNumberAlwaysAcceptsItsFixedCodeAndNothingIsSentToTheOtpSender() throws Exception {
        String mobile = "9000000001";

        post("/api/v1/auth/otp/request", null, "{\"mobile\":\"" + mobile + "\"}");

        // The usual OtpSender was never called, so a store reviewer who can't see a real code
        // leaves no trace in whatever would have delivered it.
        assertNull(otpSender.lastCode("+91" + mobile));

        assertEquals(401,
                status(post("/api/v1/auth/otp/verify", null,
                        "{\"mobile\":\"" + mobile + "\",\"code\":\"000000\"}")));

        assertEquals(200,
                status(post("/api/v1/auth/otp/verify", null,
                        "{\"mobile\":\"" + mobile + "\",\"code\":\"123456\"}")));
    }

    @Test
    void anyOtherNumberStillGetsARandomCodeDeliveredTheNormalWay() throws Exception {
        String mobile = randomMobile();

        post("/api/v1/auth/otp/request", null, "{\"mobile\":\"" + mobile + "\"}");

        String delivered = otpSender.lastCode("+91" + mobile);
        assertEquals(6, delivered.length());

        assertEquals(200,
                status(post("/api/v1/auth/otp/verify", null,
                        "{\"mobile\":\"" + mobile + "\",\"code\":\"" + delivered + "\"}")));
    }
}
