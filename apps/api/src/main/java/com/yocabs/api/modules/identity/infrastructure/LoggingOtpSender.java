package com.yocabs.api.modules.identity.infrastructure;

import com.yocabs.api.modules.identity.application.OtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development sender: writes the code to the application log instead of
 * sending an SMS. Selected by yocabs.otp.sender=logging (the default).
 */
@Component
@ConditionalOnProperty(name = "yocabs.otp.sender", havingValue = "logging", matchIfMissing = true)
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    private final boolean logCode;

    public LoggingOtpSender(@Value("${yocabs.otp.log-code:true}") boolean logCode) {
        this.logCode = logCode;
    }

    @Override
    public void send(String mobile, String code) {
        String masked = "*".repeat(Math.max(0, mobile.length() - 4))
                + mobile.substring(Math.max(0, mobile.length() - 4));

        if (logCode) {
            log.info("[DEV OTP] code {} for {}", code, masked);
        } else {
            log.info("OTP issued for {}", masked);
        }
    }
}
