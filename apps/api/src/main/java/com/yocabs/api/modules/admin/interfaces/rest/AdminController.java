package com.yocabs.api.modules.admin.interfaces.rest;

import com.yocabs.api.modules.admin.application.AdminService;
import com.yocabs.api.modules.admin.application.AdminService.Dashboard;
import com.yocabs.api.modules.audit.domain.AuditLog;
import com.yocabs.api.modules.booking.application.BookingService;
import com.yocabs.api.modules.booking.domain.model.BookingStatus;
import com.yocabs.api.modules.booking.interfaces.rest.BookingController.BookingResponse;
import com.yocabs.api.modules.booking.interfaces.rest.BookingViewAssembler;
import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.modules.identity.domain.model.UserStatus;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartnerStatus;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import com.yocabs.api.shared.security.Role;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private final BookingService bookingService;
    private final BookingViewAssembler bookingAssembler;

    public AdminController(
            AdminService adminService,
            BookingService bookingService,
            BookingViewAssembler bookingAssembler
    ) {
        this.adminService = adminService;
        this.bookingService = bookingService;
        this.bookingAssembler = bookingAssembler;
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(@CurrentActor Actor actor) {
        return adminService.dashboard(actor);
    }

    @GetMapping("/travel-partners")
    public List<PartnerResponse> partners(
            @CurrentActor Actor actor,
            @RequestParam(required = false) TravelPartnerStatus status
    ) {
        return adminService.listPartners(actor, status).stream().map(PartnerResponse::from).toList();
    }

    @PostMapping("/travel-partners/{partnerId}/activate")
    public PartnerResponse activate(@CurrentActor Actor actor, @PathVariable UUID partnerId) {
        return PartnerResponse.from(adminService.activatePartner(actor, partnerId));
    }

    @PostMapping("/travel-partners/{partnerId}/suspend")
    public PartnerResponse suspend(
            @CurrentActor Actor actor,
            @PathVariable UUID partnerId,
            @RequestBody(required = false) ReasonRequest request
    ) {
        return PartnerResponse.from(
                adminService.suspendPartner(actor, partnerId, request == null ? null : request.reason())
        );
    }

    @PostMapping("/travel-partners/{partnerId}/reinstate")
    public PartnerResponse reinstate(@CurrentActor Actor actor, @PathVariable UUID partnerId) {
        return PartnerResponse.from(adminService.reinstatePartner(actor, partnerId));
    }

    @PostMapping("/travel-partners/{partnerId}/deactivate")
    public PartnerResponse deactivate(@CurrentActor Actor actor, @PathVariable UUID partnerId) {
        return PartnerResponse.from(adminService.deactivatePartner(actor, partnerId));
    }

    @PostMapping("/travel-partners/{partnerId}/owner")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createPartnerOwner(
            @CurrentActor Actor actor,
            @PathVariable UUID partnerId,
            @RequestBody CreateOwnerRequest request
    ) {
        return UserResponse.from(
                adminService.createPartnerOwner(actor, partnerId, request.name(), request.mobile())
        );
    }

    @GetMapping("/bookings")
    public List<BookingResponse> bookings(
            @CurrentActor Actor actor,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return bookingAssembler.toResponses(bookingService.listAll(actor, status, limit), actor);
    }

    @GetMapping("/users")
    public List<UserResponse> users(@CurrentActor Actor actor, @RequestParam Role role) {
        return adminService.listUsers(actor, role).stream().map(UserResponse::from).toList();
    }

    @PostMapping("/users/{userId}/block")
    public UserResponse block(@CurrentActor Actor actor, @PathVariable UUID userId) {
        return UserResponse.from(adminService.blockUser(actor, userId));
    }

    @PostMapping("/users/{userId}/unblock")
    public UserResponse unblock(@CurrentActor Actor actor, @PathVariable UUID userId) {
        return UserResponse.from(adminService.unblockUser(actor, userId));
    }

    @PostMapping("/admins")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createAdmin(@CurrentActor Actor actor, @RequestBody CreateAdminRequest request) {
        return UserResponse.from(
                adminService.createAdmin(actor, request.email(), request.password(), request.displayName())
        );
    }

    @GetMapping("/audit-logs")
    public List<AuditLogResponse> auditLogs(
            @CurrentActor Actor actor,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return adminService.auditLogs(actor, limit).stream().map(AuditLogResponse::from).toList();
    }

    public record ReasonRequest(String reason) {
    }

    public record CreateOwnerRequest(String name, String mobile) {
    }

    public record CreateAdminRequest(String email, String password, String displayName) {
    }

    public record PartnerResponse(UUID id, String name, TravelPartnerStatus status, Instant createdAt) {

        static PartnerResponse from(TravelPartner partner) {
            return new PartnerResponse(
                    partner.getId(), partner.getName(), partner.getStatus(), partner.getCreatedAt()
            );
        }
    }

    public record UserResponse(
            UUID id,
            String mobile,
            String email,
            String displayName,
            Role role,
            UUID partnerId,
            UserStatus status,
            Instant createdAt
    ) {

        static UserResponse from(UserAccount account) {
            return new UserResponse(
                    account.getId(), account.getMobile(), account.getEmail(), account.getDisplayName(),
                    account.getRole(), account.getPartnerId(), account.getStatus(), account.getCreatedAt()
            );
        }
    }

    public record AuditLogResponse(
            UUID id,
            UUID actorId,
            Role actorRole,
            String action,
            String targetType,
            UUID targetId,
            String details,
            Instant createdAt
    ) {

        static AuditLogResponse from(AuditLog log) {
            return new AuditLogResponse(
                    log.id(), log.actorId(), log.actorRole(), log.action(),
                    log.targetType(), log.targetId(), log.details(), log.createdAt()
            );
        }
    }
}
