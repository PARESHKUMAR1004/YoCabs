package com.yocabs.api.modules.identity.application;

import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.modules.identity.domain.repository.UserAccountRepository;
import com.yocabs.api.modules.travelpartner.application.command.CreateTravelPartnerCommand;
import com.yocabs.api.modules.travelpartner.application.service.CreateTravelPartnerService;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.shared.security.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerRegistrationService {

    private final CreateTravelPartnerService createTravelPartnerService;
    private final UserAccountRepository users;

    public PartnerRegistrationService(
            CreateTravelPartnerService createTravelPartnerService,
            UserAccountRepository users
    ) {
        this.createTravelPartnerService = createTravelPartnerService;
        this.users = users;
    }

    /** Creates a partner awaiting admin approval and its owner login. */
    @Transactional
    public Registration register(String rawMobile, String ownerName, String businessName) {

        String mobile = AuthService.normalizeMobile(rawMobile);

        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("Owner name is required");
        }

        if (users.findByMobile(mobile).isPresent()) {
            throw new IllegalStateException("This mobile number is already registered");
        }

        TravelPartner partner =
                createTravelPartnerService.execute(
                        new CreateTravelPartnerCommand(businessName)
                );

        UserAccount owner =
                users.save(
                        UserAccount.newPartnerUser(
                                mobile, ownerName.trim(), Role.PARTNER_OWNER, partner.getId()
                        )
                );

        return new Registration(partner, owner);
    }

    public record Registration(TravelPartner partner, UserAccount owner) {
    }
}
