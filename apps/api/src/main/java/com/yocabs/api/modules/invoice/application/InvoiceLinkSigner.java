package com.yocabs.api.modules.invoice.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Signs a short-lived, stateless token that lets a bill be opened as a plain link (a phone's
 * browser cannot attach an Authorization header). The token carries the booking id and an expiry,
 * both protected by an HMAC, so nothing needs to be stored to issue or verify one.
 */
@Component
public class InvoiceLinkSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public InvoiceLinkSigner(@Value("${yocabs.invoice.link-secret}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String sign(UUID bookingId, Instant expiresAt) {
        String payload = bookingId + ":" + expiresAt.getEpochSecond();
        return payload + ":" + hmac(payload);
    }

    /** The booking id, only if the token's signature is valid and it has not expired. */
    public Optional<UUID> verify(String token) {

        String[] parts = token == null ? new String[0] : token.split(":");

        if (parts.length != 3) {
            return Optional.empty();
        }

        String payload = parts[0] + ":" + parts[1];

        if (!hmac(payload).equals(parts[2])) {
            return Optional.empty();
        }

        try {
            UUID bookingId = UUID.fromString(parts[0]);
            long expiresAt = Long.parseLong(parts[1]);

            if (Instant.now().getEpochSecond() > expiresAt) {
                return Optional.empty();
            }

            return Optional.of(bookingId);

        } catch (RuntimeException malformed) {
            return Optional.empty();
        }
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
