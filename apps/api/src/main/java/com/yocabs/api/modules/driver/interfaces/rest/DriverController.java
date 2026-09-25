package com.yocabs.api.modules.driver.interfaces.rest;

import com.yocabs.api.modules.driver.application.DriverService;
import com.yocabs.api.modules.driver.domain.model.Driver;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/travel-partners/{travelPartnerId}/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DriverResponse create(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @RequestBody CreateDriverRequest request
    ) {
        return DriverResponse.from(
                driverService.addDriver(
                        actor, travelPartnerId, request.name(), request.mobile(), request.licenseNumber()
                )
        );
    }

    @GetMapping
    public List<DriverResponse> list(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId
    ) {
        return driverService.list(actor, travelPartnerId).stream()
                .map(DriverResponse::from).toList();
    }

    @GetMapping("/{driverId}")
    public DriverResponse get(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID driverId
    ) {
        return DriverResponse.from(driverService.get(actor, travelPartnerId, driverId));
    }

    @PostMapping("/{driverId}/deactivate")
    public DriverResponse deactivate(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID driverId
    ) {
        return DriverResponse.from(driverService.deactivate(actor, travelPartnerId, driverId));
    }

    @PostMapping("/{driverId}/activate")
    public DriverResponse activate(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID driverId
    ) {
        return DriverResponse.from(driverService.activate(actor, travelPartnerId, driverId));
    }

    public record CreateDriverRequest(String name, String mobile, String licenseNumber) {
    }

    public record DriverResponse(
            UUID id,
            UUID travelPartnerId,
            String name,
            String mobile,
            String licenseNumber,
            Driver.Status status,
            Instant createdAt
    ) {

        static DriverResponse from(Driver driver) {
            return new DriverResponse(
                    driver.getId(),
                    driver.getTravelPartnerId(),
                    driver.getName(),
                    driver.getMobile(),
                    driver.getLicenseNumber(),
                    driver.getStatus(),
                    driver.getCreatedAt()
            );
        }
    }
}
