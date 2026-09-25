package com.yocabs.api.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter(JsonMapper.builder().build(), 3, 100);

    private MockHttpServletResponse call(String method, String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void otpRequestsBeyondTheLimitAreRejectedWithRetryAfter() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals(200, call("POST", "/api/v1/auth/otp/request", "10.0.0.1").getStatus());
        }

        MockHttpServletResponse limited = call("POST", "/api/v1/auth/otp/request", "10.0.0.1");

        assertEquals(429, limited.getStatus());
        assertEquals("60", limited.getHeader("Retry-After"));
        assertTrue(limited.getContentAsString().contains("TOO_MANY_REQUESTS"));
    }

    @Test
    void limitsAreTrackedPerClient() throws Exception {
        for (int i = 0; i < 3; i++) {
            call("POST", "/api/v1/auth/otp/request", "10.0.0.2");
        }

        assertEquals(429, call("POST", "/api/v1/auth/otp/request", "10.0.0.2").getStatus());
        assertEquals(200, call("POST", "/api/v1/auth/otp/request", "10.0.0.3").getStatus());
    }

    @Test
    void unrelatedRoutesAndReadsAreNotLimited() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertEquals(200, call("GET", "/api/v1/auth/otp/request", "10.0.0.4").getStatus());
            assertEquals(200, call("POST", "/api/v1/bookings", "10.0.0.4").getStatus());
        }
    }
}
