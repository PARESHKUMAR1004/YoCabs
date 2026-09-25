package com.yocabs.api.modules.admin.application;

import com.yocabs.api.modules.admin.infrastructure.AdminStatsQuery;
import com.yocabs.api.modules.audit.application.AuditService;
import com.yocabs.api.modules.audit.domain.AuditLog;
import com.yocabs.api.modules.booking.domain.repository.BookingRepository;
import com.yocabs.api.modules.booking.domain.repository.BookingRepository.BookingStats;
import com.yocabs.api.modules.document.application.DocumentService;
import com.yocabs.api.modules.document.domain.Document;
import com.yocabs.api.modules.identity.application.AuthService;
import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.modules.identity.domain.repository.RefreshTokenRepository;
import com.yocabs.api.modules.identity.domain.repository.UserAccountRepository;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartnerStatus;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.shared.events.NotificationRequested;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminService {

    private static final String PARTNER = "TRAVEL_PARTNER";
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final TravelPartnerRepository partners;
    private final BookingRepository bookings;
    private final DocumentService documentService;
    private final AuditService auditService;
    private final UserAccountRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AdminStatsQuery statsQuery;
    private final ApplicationEventPublisher events;
    private final List<Document.Type> requiredPartnerDocuments;

    public AdminService(
            TravelPartnerRepository partners,
            BookingRepository bookings,
            DocumentService documentService,
            AuditService auditService,
            UserAccountRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder,
            AdminStatsQuery statsQuery,
            ApplicationEventPublisher events,
            @Value("${yocabs.verification.required-partner-documents:}") String requiredDocuments
    ) {
        this.partners = partners;
        this.bookings = bookings;
        this.documentService = documentService;
        this.auditService = auditService;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.statsQuery = statsQuery;
        this.events = events;
        this.requiredPartnerDocuments =
                Arrays.stream(requiredDocuments.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .map(Document.Type::valueOf)
                        .toList();
    }

    @Transactional(readOnly = true)
    public List<TravelPartner> listPartners(Actor actor, TravelPartnerStatus status) {
        actor.requireAdmin();
        return status == null ? partners.findAll() : partners.findByStatus(status);
    }

    /** Verification gate: a partner goes live only with its required documents approved. */
    @Transactional
    public TravelPartner activatePartner(Actor actor, UUID partnerId) {

        actor.requireAdmin();
        TravelPartner partner = load(partnerId);

        List<Document.Type> missing =
                requiredPartnerDocuments.stream()
                        .filter(type ->
                                !documentService.hasApproved(Document.OwnerType.PARTNER, partnerId, type))
                        .toList();

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "The partner cannot be activated until these documents are approved: " + missing
            );
        }

        partner.activate();
        return persist(actor, partner, "PARTNER_ACTIVATED", "Your travel partner account is active",
                "Your account has been approved. You can now add vehicles and receive bookings.");
    }

    @Transactional
    public TravelPartner suspendPartner(Actor actor, UUID partnerId, String reason) {
        actor.requireAdmin();
        TravelPartner partner = load(partnerId);
        partner.suspend();
        return persist(actor, partner, "PARTNER_SUSPENDED", "Your account was suspended",
                reason == null || reason.isBlank()
                        ? "Your account has been suspended by YoCabs."
                        : "Your account has been suspended: " + reason);
    }

    @Transactional
    public TravelPartner reinstatePartner(Actor actor, UUID partnerId) {
        actor.requireAdmin();
        TravelPartner partner = load(partnerId);
        partner.reinstate();
        return persist(actor, partner, "PARTNER_REINSTATED", "Your account was reinstated",
                "Your travel partner account is active again.");
    }

    @Transactional
    public TravelPartner deactivatePartner(Actor actor, UUID partnerId) {
        actor.requireAdmin();
        TravelPartner partner = load(partnerId);
        partner.deactivate();
        return persist(actor, partner, "PARTNER_DEACTIVATED", "Your account was deactivated",
                "Your travel partner account has been deactivated.");
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard(Actor actor) {

        actor.requireAdmin();

        return new Dashboard(
                bookings.stats(),
                statsQuery.partnersByStatus(),
                statsQuery.count("vehicles", "1 = 1"),
                statsQuery.count("vehicles", "status = 'AVAILABLE'"),
                statsQuery.count("drivers", "status = 'ACTIVE'"),
                users.countByRole(Role.TOURIST),
                statsQuery.count("documents", "status = 'PENDING'"),
                statsQuery.count("payouts", "status = 'REQUESTED'"),
                statsQuery.count("support_tickets", "status in ('OPEN', 'IN_PROGRESS')"),
                Instant.now()
        );
    }

    /** Gives an admin-created partner its owner login (one owner per partner). */
    @Transactional
    public UserAccount createPartnerOwner(Actor actor, UUID partnerId, String ownerName, String rawMobile) {

        actor.requireAdmin();
        load(partnerId);

        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("Owner name is required");
        }

        if (users.findByPartnerId(partnerId).stream()
                .anyMatch(user -> user.getRole() == Role.PARTNER_OWNER)) {
            throw new IllegalStateException("This travel partner already has an owner");
        }

        String mobile = AuthService.normalizeMobile(rawMobile);

        if (users.findByMobile(mobile).isPresent()) {
            throw new IllegalStateException("This mobile number is already registered");
        }

        UserAccount owner =
                users.save(
                        UserAccount.newPartnerUser(mobile, ownerName.trim(), Role.PARTNER_OWNER, partnerId)
                );

        auditService.record(actor, "PARTNER_OWNER_CREATED", PARTNER, partnerId, "owner=" + owner.getId());
        return owner;
    }

    @Transactional(readOnly = true)
    public List<UserAccount> listUsers(Actor actor, Role role) {
        actor.requireAdmin();
        return users.findByRole(role);
    }

    @Transactional
    public UserAccount blockUser(Actor actor, UUID userId) {

        actor.requireAdmin();

        UserAccount account = loadUser(userId);

        if (account.getId().equals(actor.userId())) {
            throw new IllegalStateException("You cannot block your own account");
        }

        if (account.getRole() == Role.ADMIN || account.getRole() == Role.SUPER_ADMIN) {
            actor.requireSuperAdmin();
        }

        account.block();
        UserAccount saved = users.save(account);
        refreshTokens.revokeAllForUser(userId, Instant.now());

        auditService.record(actor, "USER_BLOCKED", "USER", userId, "role=" + saved.getRole());
        return saved;
    }

    @Transactional
    public UserAccount unblockUser(Actor actor, UUID userId) {

        actor.requireAdmin();

        UserAccount account = loadUser(userId);

        if (account.getRole() == Role.ADMIN || account.getRole() == Role.SUPER_ADMIN) {
            actor.requireSuperAdmin();
        }

        account.unblock();
        UserAccount saved = users.save(account);

        auditService.record(actor, "USER_UNBLOCKED", "USER", userId, "role=" + saved.getRole());
        return saved;
    }

    @Transactional
    public UserAccount createAdmin(Actor actor, String email, String password, String displayName) {

        actor.requireSuperAdmin();

        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email is required");
        }

        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters"
            );
        }

        if (users.findByEmail(email.trim().toLowerCase()).isPresent()) {
            throw new IllegalStateException("An account with this email already exists");
        }

        UserAccount admin =
                users.save(
                        UserAccount.newAdmin(
                                email, passwordEncoder.encode(password), displayName, Role.ADMIN
                        )
                );

        auditService.record(actor, "ADMIN_CREATED", "USER", admin.getId(), email);
        return admin;
    }

    @Transactional(readOnly = true)
    public List<AuditLog> auditLogs(Actor actor, int limit) {
        return auditService.recent(actor, limit);
    }

    private TravelPartner persist(
            Actor actor,
            TravelPartner partner,
            String action,
            String title,
            String body
    ) {
        TravelPartner saved = partners.update(partner);

        auditService.record(actor, action, PARTNER, saved.getId(), "status=" + saved.getStatus());

        events.publishEvent(
                NotificationRequested.toPartner(
                        saved.getId(), action, title, body, PARTNER, saved.getId()
                )
        );

        return saved;
    }

    private TravelPartner load(UUID partnerId) {
        return partners.findById(partnerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Travel partner not found: " + partnerId));
    }

    private UserAccount loadUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    public record Dashboard(
            BookingStats bookings,
            Map<String, Long> partnersByStatus,
            long vehicles,
            long availableVehicles,
            long activeDrivers,
            long tourists,
            long pendingDocuments,
            long pendingPayouts,
            long openSupportTickets,
            Instant generatedAt
    ) {
    }
}
