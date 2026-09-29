package com.yocabs.api.modules.standardrate.domain;

import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** The reference per-kilometre fare YoCabs quotes travellers for one vehicle type. */
public record StandardRate(
        VehicleCategory category,
        BigDecimal perKmRate,
        UUID updatedBy,
        Instant updatedAt
) {
    public StandardRate {
        if (category == null) {
            throw new IllegalArgumentException("Vehicle category is required");
        }
        if (perKmRate == null || perKmRate.signum() <= 0) {
            throw new IllegalArgumentException("The rate must be greater than zero");
        }
    }
}
