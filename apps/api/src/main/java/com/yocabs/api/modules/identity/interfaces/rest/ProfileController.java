package com.yocabs.api.modules.identity.interfaces.rest;

import com.yocabs.api.modules.identity.application.ProfileService;
import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse get(@CurrentActor Actor actor) {
        return ProfileResponse.from(profileService.get(actor));
    }

    @PutMapping
    public ProfileResponse update(
            @CurrentActor Actor actor,
            @RequestBody UpdateProfileRequest request
    ) {
        return ProfileResponse.from(
                profileService.update(
                        actor, request.displayName(), request.email(), request.preferredLanguage()
                )
        );
    }

    public record UpdateProfileRequest(String displayName, String email, String preferredLanguage) {
    }

    public record ProfileResponse(
            UUID id,
            String role,
            String mobile,
            String email,
            String displayName,
            String preferredLanguage,
            UUID partnerId,
            Instant createdAt
    ) {

        static ProfileResponse from(UserAccount account) {
            return new ProfileResponse(
                    account.getId(),
                    account.getRole().name(),
                    account.getMobile(),
                    account.getEmail(),
                    account.getDisplayName(),
                    account.getPreferredLanguage(),
                    account.getPartnerId(),
                    account.getCreatedAt()
            );
        }
    }
}
