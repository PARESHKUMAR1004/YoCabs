package com.yocabs.api.modules.standardrate.interfaces.rest;

import com.yocabs.api.modules.standardrate.application.StandardRateService;
import com.yocabs.api.modules.standardrate.domain.StandardRate;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
public class StandardRateController {

    private final StandardRateService standardRateService;

    public StandardRateController(StandardRateService standardRateService) {
        this.standardRateService = standardRateService;
    }

    /** Public, like a trip search: the app shows the benchmark before anyone signs in. */
    @GetMapping("/api/v1/standard-rates")
    public List<StandardRateResponse> list() {
        return standardRateService.list().stream().map(StandardRateResponse::from).toList();
    }

    @PutMapping("/api/v1/admin/standard-rates/{category}")
    public StandardRateResponse set(
            @CurrentActor Actor actor,
            @PathVariable VehicleCategory category,
            @RequestBody SetStandardRateRequest request
    ) {
        if (request.perKmRate() == null) {
            throw new IllegalArgumentException("A per-kilometre rate is required");
        }

        return StandardRateResponse.from(
                standardRateService.setRate(actor, category, request.perKmRate())
        );
    }

    public record SetStandardRateRequest(BigDecimal perKmRate) {
    }

    public record StandardRateResponse(
            String category,
            BigDecimal perKmRate,
            Instant updatedAt
    ) {
        static StandardRateResponse from(StandardRate rate) {
            return new StandardRateResponse(rate.category().name(), rate.perKmRate(), rate.updatedAt());
        }
    }
}
