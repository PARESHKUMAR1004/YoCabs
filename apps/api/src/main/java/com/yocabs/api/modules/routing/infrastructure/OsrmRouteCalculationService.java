package com.yocabs.api.modules.routing.infrastructure;

import com.yocabs.api.modules.routing.application.RouteCalculationService;
import com.yocabs.api.modules.routing.domain.RouteCalculation;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/** Road routing via an OSRM server (yocabs.routing.provider=osrm). */
@Component
@ConditionalOnProperty(name = "yocabs.routing.provider", havingValue = "osrm")
public class OsrmRouteCalculationService
        implements RouteCalculationService {

    private final RestClient restClient;

    public OsrmRouteCalculationService(
            RestClient.Builder builder,
            @Value("${yocabs.routing.osrm.base-url}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public RouteCalculation calculate(Itinerary itinerary) {

        List<Location> points =
                HaversineRouteCalculationService.RoutePoints.of(itinerary);

        String coordinates =
                points.stream()
                        .map(p -> p.longitude() + "," + p.latitude())
                        .collect(Collectors.joining(";"));

        OsrmResponse response;
        try {
            response = restClient.get()
                    .uri("/route/v1/driving/{coordinates}?overview=false", coordinates)
                    .retrieve()
                    .body(OsrmResponse.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "Routing provider is unavailable", exception
            );
        }

        if (response == null
                || !"Ok".equals(response.code())
                || response.routes() == null
                || response.routes().isEmpty()) {
            throw new IllegalStateException(
                    "Routing provider could not calculate a route"
            );
        }

        OsrmRoute route = response.routes().getFirst();

        return new RouteCalculation(
                BigDecimal.valueOf(route.distance() / 1000.0)
                        .setScale(2, RoundingMode.HALF_UP),
                Duration.ofSeconds(Math.max(60, Math.round(route.duration())))
        );
    }

    record OsrmResponse(String code, List<OsrmRoute> routes) {
    }

    record OsrmRoute(double distance, double duration) {
    }
}
