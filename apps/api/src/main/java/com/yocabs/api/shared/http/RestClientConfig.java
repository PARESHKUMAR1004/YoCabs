package com.yocabs.api.shared.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Supplies the {@link RestClient.Builder} the outbound adapters build their clients from.
 *
 * Spring Boot does not auto-configure one in this application, and without it any provider that
 * calls out over HTTP (routing, and later payments or SMS) fails at startup rather than at the
 * first request. Guarded by {@code @ConditionalOnMissingBean} so a future Boot version that does
 * provide one wins.
 */
@Configuration
public class RestClientConfig {

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder(
            @Value("${yocabs.http.timeout-ms:5000}") int timeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        /*
         * Timeouts live here rather than in each adapter: an adapter that sets its own
         * request factory discards whatever the caller configured, which makes it
         * impossible to put a test double in front of it.
         */
        return RestClient.builder().requestFactory(requestFactory);
    }
}
