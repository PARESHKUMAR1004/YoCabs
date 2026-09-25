package com.yocabs.api.modules.identity.interfaces.rest;

import com.yocabs.api.modules.identity.application.StaffService;
import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/travel-partners/{travelPartnerId}/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse add(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @RequestBody AddStaffRequest request
    ) {
        return StaffResponse.from(
                staffService.addStaff(actor, travelPartnerId, request.name(), request.mobile())
        );
    }

    @GetMapping
    public List<StaffResponse> list(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId
    ) {
        return staffService.listStaff(actor, travelPartnerId).stream()
                .map(StaffResponse::from).toList();
    }

    @DeleteMapping("/{staffId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID staffId
    ) {
        staffService.removeStaff(actor, travelPartnerId, staffId);
    }

    public record AddStaffRequest(String name, String mobile) {
    }

    public record StaffResponse(UUID id, String name, String mobile, String status, Instant createdAt) {

        static StaffResponse from(UserAccount account) {
            return new StaffResponse(
                    account.getId(), account.getDisplayName(), account.getMobile(),
                    account.getStatus().name(), account.getCreatedAt()
            );
        }
    }
}
