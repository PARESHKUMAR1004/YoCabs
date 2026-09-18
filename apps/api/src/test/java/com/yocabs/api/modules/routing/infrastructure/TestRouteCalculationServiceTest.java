package com.yocabs.api.modules.routing.infrastructure;

import com.yocabs.api.modules.routing.domain.RouteCalculation;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestRouteCalculationServiceTest {

    private final TestRouteCalculationService service =
            new TestRouteCalculationService();

    @Test
    void shouldReturnRouteCalculation() {

        Itinerary itinerary =
                new Itinerary(
                        new Location(
                                "Bhubaneswar Airport",
                                20.2444,
                                85.8178
                        ),
                        List.of(
                                new Location(
                                        "Konark",
                                        19.8876,
                                        86.0945
                                )
                        ),
                        new Location(
                                "Puri",
                                19.8135,
                                85.8312
                        )
                );

        RouteCalculation result =
                service.calculate(
                        itinerary
                );

        assertEquals(
                BigDecimal.valueOf(100),
                result.distanceKm()
        );

        assertEquals(
                120,
                result.duration().toMinutes()
        );
    }
}