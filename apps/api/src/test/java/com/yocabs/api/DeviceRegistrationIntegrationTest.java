package com.yocabs.api;

import com.yocabs.api.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeviceRegistrationIntegrationTest extends IntegrationTestBase {

    @Test
    void aSignedInUserCanRegisterAndForgetAPhone() throws Exception {
        String tourist = otpLogin(randomMobile());
        String body = "{\"token\":\"ExponentPushToken[abc123]\",\"platform\":\"android\"}";

        assertEquals(204, status(post("/api/v1/devices", tourist, body)));
        // Registering again is harmless: the phone simply signs in again.
        assertEquals(204, status(post("/api/v1/devices", tourist, body)));

        assertEquals(204, status(delete("/api/v1/devices?token=ExponentPushToken[abc123]", tourist)));
    }

    @Test
    void onlyExpoPushTokensAreAccepted() throws Exception {
        String tourist = otpLogin(randomMobile());

        assertEquals(400, status(post("/api/v1/devices", tourist, "{\"token\":\"not-a-token\"}")));
        assertEquals(400, status(post("/api/v1/devices", tourist, "{}")));
    }

    @Test
    void registeringAPhoneNeedsSigningIn() throws Exception {
        assertEquals(401, status(post("/api/v1/devices", null, "{\"token\":\"ExponentPushToken[abc]\"}")));
    }
}
