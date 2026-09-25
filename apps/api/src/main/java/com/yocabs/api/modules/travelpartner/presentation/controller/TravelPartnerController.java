package com.yocabs.api.modules.travelpartner.presentation.controller;

import com.yocabs.api.modules.admin.application.AdminService;
import com.yocabs.api.modules.travelpartner.application.command.CreateTravelPartnerCommand;
import com.yocabs.api.modules.travelpartner.application.service.CreateTravelPartnerService;
import com.yocabs.api.modules.travelpartner.application.service.GetTravelPartnerService;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/travel-partners")
public class TravelPartnerController {

    private final CreateTravelPartnerService createService;
    private final GetTravelPartnerService getService;
    private final AdminService adminService;

    public TravelPartnerController(
            CreateTravelPartnerService createService,
            GetTravelPartnerService getService,
            AdminService adminService
    ) {
        this.createService = createService;
        this.getService = getService;
        this.adminService = adminService;
    }

    /** Admin-created partner (self-service registration is POST /auth/partner/register). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UUID create(
            @CurrentActor Actor actor,
            @RequestBody CreateTravelPartnerRequest request
    ) {

        actor.requireAdmin();

        return createService
                .execute(
                        new CreateTravelPartnerCommand(
                                request.name()
                        )
                )
                .getId();
    }

    /**
     * The partner's own organisation and its status (awaiting approval / active ...).
     * Service areas belong to each vehicle, under /travel-partners/{id}/vehicles.
     */
    @GetMapping("/{id}")
    public TravelPartnerResponse get(
            @CurrentActor Actor actor,
            @PathVariable UUID id
    ) {
        return TravelPartnerResponse.from(getService.execute(actor, id));
    }

    /** Goes through the admin verification gate (required documents approved). */
    @PostMapping("/{id}/activate")
    public UUID activate(
            @CurrentActor Actor actor,
            @PathVariable UUID id
    ) {

        return adminService
                .activatePartner(actor, id)
                .getId();
    }

    public record TravelPartnerResponse(
            UUID id,
            String name,
            String status,
            Instant createdAt
    ) {

        static TravelPartnerResponse from(TravelPartner partner) {
            return new TravelPartnerResponse(
                    partner.getId(),
                    partner.getName(),
                    partner.getStatus().name(),
                    partner.getCreatedAt()
            );
        }
    }

    public record CreateTravelPartnerRequest(
            String name
    ) {
    }
}
