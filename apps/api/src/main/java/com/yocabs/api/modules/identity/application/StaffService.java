package com.yocabs.api.modules.identity.application;

import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.modules.identity.domain.repository.RefreshTokenRepository;
import com.yocabs.api.modules.identity.domain.repository.UserAccountRepository;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartnerStatus;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Organisation-scoped staff accounts managed by the partner owner. */
@Service
public class StaffService {

    private final UserAccountRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final TravelPartnerRepository partners;

    public StaffService(
            UserAccountRepository users,
            RefreshTokenRepository refreshTokens,
            TravelPartnerRepository partners
    ) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.partners = partners;
    }

    @Transactional
    public UserAccount addStaff(Actor actor, UUID travelPartnerId, String name, String rawMobile) {

        requireOwnerOrAdmin(actor, travelPartnerId);

        partners.findById(travelPartnerId)
                .filter(partner -> partner.getStatus() == TravelPartnerStatus.ACTIVE)
                .orElseThrow(() ->
                        new IllegalStateException("Staff can only be added to an active travel partner"));

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Staff name is required");
        }

        String mobile = AuthService.normalizeMobile(rawMobile);

        if (users.findByMobile(mobile).isPresent()) {
            throw new IllegalStateException("This mobile number is already registered");
        }

        return users.save(
                UserAccount.newPartnerUser(mobile, name.trim(), Role.PARTNER_STAFF, travelPartnerId)
        );
    }

    @Transactional(readOnly = true)
    public List<UserAccount> listStaff(Actor actor, UUID travelPartnerId) {
        requireOwnerOrAdmin(actor, travelPartnerId);
        return users.findByPartnerId(travelPartnerId).stream()
                .filter(user -> user.getRole() == Role.PARTNER_STAFF)
                .toList();
    }

    @Transactional
    public UserAccount removeStaff(Actor actor, UUID travelPartnerId, UUID staffId) {

        requireOwnerOrAdmin(actor, travelPartnerId);

        UserAccount staff =
                users.findById(staffId)
                        .filter(user -> user.getRole() == Role.PARTNER_STAFF
                                && travelPartnerId.equals(user.getPartnerId()))
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Staff member not found: " + staffId));

        staff.block();
        UserAccount saved = users.save(staff);
        refreshTokens.revokeAllForUser(staffId, Instant.now());
        return saved;
    }

    private void requireOwnerOrAdmin(Actor actor, UUID travelPartnerId) {
        actor.requirePartnerAccess(travelPartnerId);

        if (!actor.isAdmin() && !actor.isOwnerOfPartner()) {
            throw new AccessDeniedException("Only the partner owner can manage staff");
        }
    }
}
