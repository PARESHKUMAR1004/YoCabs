package com.yocabs.api.modules.pricing.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public record PricingContext(
        BigDecimal distanceKm,
        Duration duration,
        Instant startTime,
        Instant endTime
) {

    public PricingContext {

        if (distanceKm != null
                && distanceKm.signum() < 0) {

            throw new IllegalArgumentException(
                    "Distance cannot be negative"
            );
        }

        if (duration != null
                && duration.isNegative()) {

            throw new IllegalArgumentException(
                    "Duration cannot be negative"
            );
        }
    }
}