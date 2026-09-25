package com.yocabs.api.shared.security;

import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

/** The authenticated caller, derived from the verified access token. */
public record Actor(
        UUID userId,
        Role role,
        UUID partnerId
) {

    public Actor {
        if (userId == null || role == null) {
            throw new IllegalArgumentException("Actor requires a user id and role");
        }
    }

    public boolean isAdmin() {
        return role == Role.ADMIN || role == Role.SUPER_ADMIN;
    }

    public boolean isPartnerUser() {
        return role == Role.PARTNER_OWNER || role == Role.PARTNER_STAFF;
    }

    public boolean isOwnerOfPartner() {
        return role == Role.PARTNER_OWNER;
    }

    public void requireRole(Role... allowed) {
        for (Role candidate : allowed) {
            if (candidate == role) {
                return;
            }
        }
        throw new AccessDeniedException("Role " + role + " is not permitted");
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Administrator access required");
        }
    }

    public void requireSuperAdmin() {
        if (role != Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Super administrator access required");
        }
    }

    /** Admins may act on any partner; partner users only on their own organisation. */
    public void requirePartnerAccess(UUID travelPartnerId) {
        if (isAdmin()) {
            return;
        }
        if (isPartnerUser() && partnerId != null && partnerId.equals(travelPartnerId)) {
            return;
        }
        throw new AccessDeniedException("Not permitted for this travel partner");
    }

    public void requireTouristOwnerOrAdmin(UUID touristId) {
        if (isAdmin()) {
            return;
        }
        requireTouristIs(touristId);
    }

    public void requireTouristIs(UUID touristId) {
        if (role == Role.TOURIST && userId.equals(touristId)) {
            return;
        }
        throw new AccessDeniedException("Not permitted for this tourist");
    }
}
