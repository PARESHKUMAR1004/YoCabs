package com.yocabs.api.modules.routing.infrastructure;

import com.yocabs.api.modules.routing.application.RouteCalculationService;
import com.yocabs.api.modules.routing.domain.RouteCalculation;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;

import java.math.BigDecimal;
import java.time.Duration;

public class TestRouteCalculationService
        implements RouteCalculationService {

    @Override
    public RouteCalculation calculate(
            Itinerary itinerary
    ) {

        if (itinerary == null) {
            throw new IllegalArgumentException(
                    "Itinerary is required"
            );
        }

        return new RouteCalculation(
                BigDecimal.valueOf(100),
                Duration.ofMinutes(120)
        );
    }
}