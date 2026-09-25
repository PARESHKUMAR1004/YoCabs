package com.yocabs.api.modules.identity.interfaces.rest;

import com.yocabs.api.modules.identity.application.AuthService;
import com.yocabs.api.modules.identity.application.AuthService.AuthTokens;
import com.yocabs.api.modules.identity.application.PartnerRegistrationService;
import com.yocabs.api.modules.identity.application.PartnerRegistrationService.Registration;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PartnerRegistrationService partnerRegistrationService;

    public AuthController(
            AuthService authService,
            PartnerRegistrationService partnerRegistrationService
    ) {
        this.authService = authService;
        this.partnerRegistrationService = partnerRegistrationService;
    }

    @PostMapping("/otp/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse requestOtp(@RequestBody OtpRequest request) {
        authService.requestOtp(request.mobile());
        return new MessageResponse("If the number is valid, a code has been sent");
    }

    @PostMapping("/otp/verify")
    public TokenResponse verifyOtp(@RequestBody OtpVerifyRequest request) {
        return TokenResponse.from(authService.verifyOtp(request.mobile(), request.code()));
    }

    @PostMapping("/admin/login")
    public TokenResponse adminLogin(@RequestBody AdminLoginRequest request) {
        return TokenResponse.from(authService.adminLogin(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody RefreshRequest request) {
        return TokenResponse.from(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @PostMapping("/partner/register")
    @ResponseStatus(HttpStatus.CREATED)
    public PartnerRegistrationResponse registerPartner(@RequestBody PartnerRegistrationRequest request) {
        Registration registration =
                partnerRegistrationService.register(
                        request.mobile(), request.ownerName(), request.businessName()
                );
        return new PartnerRegistrationResponse(
                registration.partner().getId(),
                registration.partner().getStatus().name()
        );
    }

    @GetMapping("/me")
    public MeResponse me(@CurrentActor Actor actor) {
        return new MeResponse(actor.userId(), actor.role().name(), actor.partnerId());
    }

    public record OtpRequest(String mobile) {
    }

    public record OtpVerifyRequest(String mobile, String code) {
    }

    public record AdminLoginRequest(String email, String password) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record PartnerRegistrationRequest(String mobile, String ownerName, String businessName) {
    }

    public record PartnerRegistrationResponse(UUID travelPartnerId, String status) {
    }

    public record MessageResponse(String message) {
    }

    public record MeResponse(UUID userId, String role, UUID partnerId) {
    }

    public record TokenResponse(
            String accessToken,
            Instant accessTokenExpiresAt,
            String refreshToken,
            UUID userId,
            String role,
            UUID partnerId
    ) {

        static TokenResponse from(AuthTokens tokens) {
            return new TokenResponse(
                    tokens.accessToken(),
                    tokens.accessTokenExpiresAt(),
                    tokens.refreshToken(),
                    tokens.userId(),
                    tokens.role().name(),
                    tokens.partnerId()
            );
        }
    }
}
