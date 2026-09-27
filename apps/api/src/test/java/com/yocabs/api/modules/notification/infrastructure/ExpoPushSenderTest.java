package com.yocabs.api.modules.notification.infrastructure;

import com.yocabs.api.modules.notification.application.DeviceTokenRepository;
import com.yocabs.api.modules.notification.application.PushSender.PushMessage;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExpoPushSenderTest {

    private static final String URL = "https://exp.host/--/api/v2/push/send";
    private static final UUID USER = UUID.randomUUID();

    private final List<String> tokens = new ArrayList<>();
    private final List<String> discarded = new ArrayList<>();

    private final DeviceTokenRepository devices = new DeviceTokenRepository() {
        @Override public void register(UUID userId, String token, String platform) { }
        @Override public void unregister(UUID userId, String token) { }
        @Override public void discard(String token) { discarded.add(token); }
        @Override public List<String> findTokens(UUID userId) { return tokens; }
    };

    private MockRestServiceServer server;

    private ExpoPushSender sender() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        // Runs the delivery on the calling thread so the test can check it straight away.
        return new ExpoPushSender(builder, devices, URL, "", Runnable::run);
    }

    private PushMessage offer() {
        return new PushMessage(USER, "NEGOTIATION_OFFER_RECEIVED", "New offer", "A traveller made an offer",
                "NEGOTIATION", UUID.fromString("00000000-0000-0000-0000-000000000042"));
    }

    @Test
    void aMessageGoesToEveryPhoneOfTheUserOnTheChannelForItsType() {
        tokens.add("ExponentPushToken[aaa]");
        tokens.add("ExponentPushToken[bbb]");
        ExpoPushSender sender = sender();

        server.expect(requestTo(URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].to").value("ExponentPushToken[aaa]"))
                .andExpect(jsonPath("$[1].to").value("ExponentPushToken[bbb]"))
                .andExpect(jsonPath("$[0].channelId").value("offers"))
                .andExpect(jsonPath("$[0].title").value("New offer"))
                .andExpect(jsonPath("$[0].data.referenceType").value("NEGOTIATION"))
                .andExpect(jsonPath("$[0].data.referenceId").value("00000000-0000-0000-0000-000000000042"))
                .andRespond(withSuccess("{\"data\":[{\"status\":\"ok\"},{\"status\":\"ok\"}]}", MediaType.APPLICATION_JSON));

        sender.send(offer());

        server.verify();
        assertEquals(0, discarded.size());
    }

    @Test
    void aPhoneThatNoLongerHasTheAppIsForgotten() {
        tokens.add("ExponentPushToken[live]");
        tokens.add("ExponentPushToken[gone]");
        ExpoPushSender sender = sender();

        server.expect(requestTo(URL))
                .andRespond(withSuccess(
                        "{\"data\":[{\"status\":\"ok\"},{\"status\":\"error\",\"message\":\"not registered\","
                                + "\"details\":{\"error\":\"DeviceNotRegistered\"}}]}",
                        MediaType.APPLICATION_JSON));

        sender.send(offer());

        assertEquals(List.of("ExponentPushToken[gone]"), discarded);
    }

    @Test
    void nothingIsSentWhenTheUserHasNoPhoneRegistered() {
        ExpoPushSender sender = sender();

        sender.send(offer());

        server.verify();
    }

    @Test
    void aFailingPushServiceNeverBreaksTheCaller() {
        tokens.add("ExponentPushToken[aaa]");
        ExpoPushSender sender = sender();

        server.expect(requestTo(URL)).andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        sender.send(offer());

        server.verify();
    }
}
