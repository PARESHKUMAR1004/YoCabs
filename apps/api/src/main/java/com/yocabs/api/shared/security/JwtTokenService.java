package com.yocabs.api.shared.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Issues short-lived HS256 access tokens. */
public class JwtTokenService {

    public static final String ROLE_CLAIM = "role";
    public static final String PARTNER_CLAIM = "partnerId";
    private static final String ISSUER = "yocabs-api";

    private final JwtEncoder encoder;
    private final Duration accessTokenTtl;

    public JwtTokenService(SecretKey secretKey, Duration accessTokenTtl) {
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        this.accessTokenTtl = accessTokenTtl;
    }

    public IssuedToken issue(UUID userId, Role role, UUID partnerId) {

        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenTtl);

        JwtClaimsSet.Builder claims =
                JwtClaimsSet.builder()
                        .issuer(ISSUER)
                        .subject(userId.toString())
                        .issuedAt(now)
                        .expiresAt(expiresAt)
                        .claim(ROLE_CLAIM, role.name());

        if (partnerId != null) {
            claims.claim(PARTNER_CLAIM, partnerId.toString());
        }

        String token =
                encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(),
                                claims.build()
                        )
                ).getTokenValue();

        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
