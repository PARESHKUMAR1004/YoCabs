package com.yocabs.api.modules.routing.domain;

import java.math.BigDecimal;
import java.time.Duration;

public record RouteCalculation(
        BigDecimal distanceKm,
        Duration duration
) {

    public RouteCalculation {

        if (distanceKm == null) {
            throw new IllegalArgumentException(
                    "Distance is required"
            );
        }

        if (distanceKm.signum() < 0) {
            throw new IllegalArgumentException(
                    "Distance cannot be negative"
            );
        }

        if (duration == null) {
            throw new IllegalArgumentException(
                    "Duration is required"
            );
        }

        if (duration.isZero()
                || duration.isNegative()) {

            throw new IllegalArgumentException(
                    "Duration must be greater than zero"
            );
        }
    }
}