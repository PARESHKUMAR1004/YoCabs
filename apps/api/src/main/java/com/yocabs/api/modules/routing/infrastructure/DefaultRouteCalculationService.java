package com.yocabs.api.modules.routing.infrastructure;

import com.yocabs.api.modules.routing.application.RouteCalculationService;
import com.yocabs.api.modules.routing.domain.RouteCalculation;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

/** Fixed 100 km / 120 min route; only for demos (yocabs.routing.provider=fixed). */
@Component
@ConditionalOnProperty(name = "yocabs.routing.provider", havingValue = "fixed")
public class DefaultRouteCalculationService
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

        /*
         * Temporary deterministic implementation.
         *
         * This will be replaced by a real routing
         * provider adapter later.
         */
        return new RouteCalculation(
                BigDecimal.valueOf(100),
                Duration.ofMinutes(120)
        );
    }
}