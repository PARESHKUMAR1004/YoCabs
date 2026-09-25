package com.yocabs.api.support;

import com.yocabs.api.modules.identity.application.OtpSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration(proxyBeanMethods = false)
public class TestSupportConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
    }

    @Bean
    @Primary
    CapturingOtpSender capturingOtpSender() {
        return new CapturingOtpSender();
    }

    /** Remembers the latest code per mobile so tests can complete the login. */
    public static class CapturingOtpSender implements OtpSender {

        private final Map<String, String> codes = new ConcurrentHashMap<>();

        @Override
        public void send(String mobile, String code) {
            codes.put(mobile, code);
        }

        public String lastCode(String mobile) {
            return codes.get(mobile);
        }
    }
}
