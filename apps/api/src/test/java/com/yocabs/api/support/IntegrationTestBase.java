package com.yocabs.api.support;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootTest(
        properties = {
                "yocabs.bootstrap-admin.email=root@yocabs.test",
                "yocabs.bootstrap-admin.password=Sup3r-Secret-Passw0rd",
                "yocabs.otp.resend-interval-seconds=0",
                "yocabs.rate-limit.otp-per-minute=1000",
                "yocabs.rate-limit.search-per-minute=1000",
                "yocabs.storage.local-path=build/test-documents",
                "yocabs.routing.provider=haversine"
        }
)
@AutoConfigureMockMvc
@Import(TestSupportConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationTestBase {

    protected static final String ADMIN_EMAIL = "root@yocabs.test";
    protected static final String ADMIN_PASSWORD = "Sup3r-Secret-Passw0rd";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected TestSupportConfiguration.CapturingOtpSender otpSender;

    protected static LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Kolkata"));
    }

    protected static String randomMobile() {
        return "9" + String.format("%09d", ThreadLocalRandom.current().nextLong(1_000_000_000L));
    }

    protected static String idempotencyKey() {
        return UUID.randomUUID().toString();
    }

    protected MvcResult perform(RequestBuilder request) throws Exception {
        return mockMvc.perform(request).andReturn();
    }

    protected MvcResult post(String path, String token, String json) throws Exception {
        return perform(withAuth(
                MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON).content(json),
                token));
    }

    protected MvcResult postWithHeader(
            String path, String token, String json, String header, String value
    ) throws Exception {
        return perform(withAuth(
                MockMvcRequestBuilders.post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(header, value)
                        .content(json),
                token));
    }

    protected MvcResult get(String path, String token) throws Exception {
        return perform(withAuth(MockMvcRequestBuilders.get(path), token));
    }

    protected MvcResult delete(String path, String token) throws Exception {
        return perform(withAuth(MockMvcRequestBuilders.delete(path), token));
    }

    protected MvcResult patch(String path, String token) throws Exception {
        return perform(withAuth(MockMvcRequestBuilders.patch(path), token));
    }

    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder request, String token) {
        return token == null ? request : request.header("Authorization", "Bearer " + token);
    }

    protected static <T> T json(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    protected static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    protected static int status(MvcResult result) {
        return result.getResponse().getStatus();
    }

    protected String adminToken() throws Exception {
        MvcResult result =
                post("/api/v1/auth/admin/login", null,
                        "{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}");
        return json(result, "$.accessToken");
    }

    /** OTP login for any mobile user (creates a tourist on first login). */
    protected String otpLogin(String mobile) throws Exception {
        post("/api/v1/auth/otp/request", null, "{\"mobile\":\"" + mobile + "\"}");

        String code = otpSender.lastCode("+91" + mobile);

        MvcResult result =
                post("/api/v1/auth/otp/verify", null,
                        "{\"mobile\":\"" + mobile + "\",\"code\":\"" + code + "\"}");
        return json(result, "$.accessToken");
    }

    protected String touristToken() throws Exception {
        return otpLogin(randomMobile());
    }
}
