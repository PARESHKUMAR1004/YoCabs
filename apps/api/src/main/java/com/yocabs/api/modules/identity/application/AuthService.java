package com.yocabs.api.modules.identity.application;

import com.yocabs.api.modules.identity.domain.model.OtpChallenge;
import com.yocabs.api.modules.identity.domain.model.RefreshToken;
import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.modules.identity.domain.repository.OtpChallengeRepository;
import com.yocabs.api.modules.identity.domain.repository.RefreshTokenRepository;
import com.yocabs.api.modules.identity.domain.repository.UserAccountRepository;
import com.yocabs.api.shared.exception.TooManyRequestsException;
import com.yocabs.api.shared.security.JwtTokenService;
import com.yocabs.api.shared.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern INDIAN_LOCAL = Pattern.compile("^[6-9][0-9]{9}$");
    private static final Pattern E164 = Pattern.compile("^\\+[1-9][0-9]{9,14}$");
    private static final String INVALID_CODE = "Invalid or expired code";
    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    private final UserAccountRepository users;
    private final OtpChallengeRepository otpChallenges;
    private final RefreshTokenRepository refreshTokens;
    private final OtpSender otpSender;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final SecretKey hmacKey;
    private final SecureRandom secureRandom = new SecureRandom();

    private final Duration otpValidity;
    private final int otpMaxAttempts;
    private final Duration otpResendInterval;
    private final Duration refreshValidity;
    private final int adminMaxFailedLogins;
    private final Duration adminLockDuration;
    private final String dummyHash;

    public AuthService(
            UserAccountRepository users,
            OtpChallengeRepository otpChallenges,
            RefreshTokenRepository refreshTokens,
            OtpSender otpSender,
            JwtTokenService jwtTokenService,
            PasswordEncoder passwordEncoder,
            SecretKey jwtSecretKey,
            @Value("${yocabs.otp.validity-minutes:5}") long otpValidityMinutes,
            @Value("${yocabs.otp.max-attempts:5}") int otpMaxAttempts,
            @Value("${yocabs.otp.resend-interval-seconds:30}") long otpResendSeconds,
            @Value("${yocabs.security.refresh-token-days:30}") long refreshDays,
            @Value("${yocabs.security.admin-max-failed-logins:5}") int adminMaxFailedLogins,
            @Value("${yocabs.security.admin-lock-minutes:15}") long adminLockMinutes
    ) {
        this.users = users;
        this.otpChallenges = otpChallenges;
        this.refreshTokens = refreshTokens;
        this.otpSender = otpSender;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.hmacKey = jwtSecretKey;
        this.otpValidity = Duration.ofMinutes(otpValidityMinutes);
        this.otpMaxAttempts = otpMaxAttempts;
        this.otpResendInterval = Duration.ofSeconds(otpResendSeconds);
        this.refreshValidity = Duration.ofDays(refreshDays);
        this.adminMaxFailedLogins = adminMaxFailedLogins;
        this.adminLockDuration = Duration.ofMinutes(adminLockMinutes);
        this.dummyHash = passwordEncoder.encode("timing-equalisation-only");
    }

    public static String normalizeMobile(String mobile) {

        if (mobile == null) {
            throw new IllegalArgumentException("Mobile number is required");
        }

        String cleaned = mobile.replaceAll("[\\s-]", "");

        if (INDIAN_LOCAL.matcher(cleaned).matches()) {
            cleaned = "+91" + cleaned;
        }

        if (!E164.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("Enter a valid mobile number");
        }

        return cleaned;
    }

    @Transactional
    public void requestOtp(String rawMobile) {

        String mobile = normalizeMobile(rawMobile);
        Instant now = Instant.now();

        otpChallenges.findLatestByMobile(mobile)
                .filter(latest -> latest.getCreatedAt().plus(otpResendInterval).isAfter(now))
                .ifPresent(latest -> {
                    throw new TooManyRequestsException(
                            "Please wait before requesting another code"
                    );
                });

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));

        otpChallenges.save(
                OtpChallenge.issue(mobile, hashCode(mobile, code), otpValidity)
        );

        otpSender.send(mobile, code);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthTokens verifyOtp(String rawMobile, String code) {

        String mobile = normalizeMobile(rawMobile);
        Instant now = Instant.now();

        if (code == null || code.isBlank()) {
            throw new BadCredentialsException(INVALID_CODE);
        }

        OtpChallenge challenge =
                otpChallenges.findLatestByMobile(mobile)
                        .filter(candidate -> candidate.isUsable(now, otpMaxAttempts))
                        .orElseThrow(() -> new BadCredentialsException(INVALID_CODE));

        challenge.registerAttempt();

        boolean matches =
                MessageDigest.isEqual(
                        hashCode(mobile, code.trim()).getBytes(StandardCharsets.UTF_8),
                        challenge.getCodeHash().getBytes(StandardCharsets.UTF_8)
                );

        if (!matches) {
            otpChallenges.save(challenge);
            throw new BadCredentialsException(INVALID_CODE);
        }

        challenge.consume(now);
        otpChallenges.save(challenge);

        UserAccount account =
                users.findByMobile(mobile)
                        .orElseGet(() -> users.save(UserAccount.newTourist(mobile)));

        requireActive(account);

        return issueTokens(account);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthTokens adminLogin(String rawEmail, String password) {

        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        Instant now = Instant.now();

        UserAccount account =
                users.findByEmail(email)
                        .filter(candidate ->
                                candidate.getRole() == Role.ADMIN
                                        || candidate.getRole() == Role.SUPER_ADMIN)
                        .orElse(null);

        if (account == null || password == null) {
            passwordEncoder.matches(String.valueOf(password), dummyHash);
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        if (account.isLocked(now) || !account.isActive()) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            account.registerFailedLogin(now, adminMaxFailedLogins, adminLockDuration);
            users.save(account);
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        account.registerSuccessfulLogin(now);
        users.save(account);

        return issueTokens(account);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthTokens refresh(String refreshTokenValue) {

        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        Instant now = Instant.now();

        RefreshToken stored =
                refreshTokens.findByTokenHash(sha256(refreshTokenValue))
                        .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (stored.isRevoked()) {
            // A rotated token was presented again: assume theft, kill the whole family.
            refreshTokens.revokeAllForUser(stored.getUserId(), now);
            throw new BadCredentialsException("Invalid refresh token");
        }

        if (!stored.isActive(now)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        stored.revoke(now);
        refreshTokens.save(stored);

        UserAccount account =
                users.findById(stored.getUserId())
                        .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        requireActive(account);

        return issueTokens(account);
    }

    @Transactional
    public void logout(String refreshTokenValue) {

        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }

        refreshTokens.findByTokenHash(sha256(refreshTokenValue))
                .ifPresent(token -> {
                    token.revoke(Instant.now());
                    refreshTokens.save(token);
                });
    }

    private AuthTokens issueTokens(UserAccount account) {

        JwtTokenService.IssuedToken access =
                jwtTokenService.issue(account.getId(), account.getRole(), account.getPartnerId());

        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String refreshValue = Base64.getUrlEncoder().withoutPadding().encodeToString(random);

        refreshTokens.save(
                RefreshToken.issue(account.getId(), sha256(refreshValue), refreshValidity)
        );

        return new AuthTokens(
                access.value(),
                access.expiresAt(),
                refreshValue,
                account.getId(),
                account.getRole(),
                account.getPartnerId()
        );
    }

    private void requireActive(UserAccount account) {
        if (!account.isActive()) {
            throw new AccessDeniedException("Account is blocked");
        }
    }

    private String hashCode(String mobile, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            return HexFormat.of().formatHex(
                    mac.doFinal((mobile + ":" + code).getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash one-time code", exception);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record AuthTokens(
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            UUID userId,
            Role role,
            UUID partnerId
    ) {
    }
}
