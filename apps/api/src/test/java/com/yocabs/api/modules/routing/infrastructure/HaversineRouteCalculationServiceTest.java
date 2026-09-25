package com.yocabs.api.modules.routing.infrastructure;

import com.yocabs.api.modules.routing.domain.RouteCalculation;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HaversineRouteCalculationServiceTest {

    private final HaversineRouteCalculationService service =
            new HaversineRouteCalculationService(1.3, 40);

    private static final Location BBSR = new Location("Bhubaneswar", 20.2961, 85.8245);
    private static final Location KONARK = new Location("Konark", 19.8876, 86.0945);
    private static final Location PURI = new Location("Puri", 19.8135, 85.8312);

    @Test
    void shouldCalculateRealisticDistanceBetweenBhubaneswarAndPuri() {

        RouteCalculation route =
                service.calculate(new Itinerary(BBSR, List.of(), PURI));

        double km = route.distanceKm().doubleValue();
        assertTrue(km > 55 && km < 80, "unexpected distance " + km);
        assertTrue(route.duration().toMinutes() > 60);
    }

    @Test
    void shouldRouteTheWholeJourneyIncludingStops() {

        RouteCalculation direct =
                service.calculate(new Itinerary(BBSR, List.of(), PURI));

        RouteCalculation viaKonark =
                service.calculate(new Itinerary(BBSR, List.of(KONARK), PURI));

        assertTrue(viaKonark.distanceKm().compareTo(direct.distanceKm()) > 0);
    }

    @Test
    void shouldRejectMissingCoordinates() {

        Itinerary itinerary =
                new Itinerary(new Location("Somewhere", null, null), List.of(), PURI);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculate(itinerary)
        );
    }
}
