package com.yocabs.api.modules.routing.infrastructure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yocabs.api.modules.routing.application.RouteCalculationService;
import com.yocabs.api.modules.routing.domain.RouteCalculation;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

/**
 * Road distance and driving time from the Google Routes API
 * (yocabs.routing.provider=google).
 *
 * This is the only routing provider whose numbers match the map the traveller
 * is looking at, and the only one that accounts for traffic. It is also the
 * only one that costs money per request, so the key belongs to the server and
 * never to the apps.
 */
@Component
@ConditionalOnProperty(
        name = "yocabs.routing.provider",
        havingValue = "google"
)
public class GoogleRoutesCalculationService
        implements RouteCalculationService {

    private static final String ENDPOINT =
            "https://routes.googleapis.com";

    /*
     * Routes bills by the fields you ask for, so ask only for the two the
     * fare needs. Requesting the full route geometry would cost more.
     */
    private static final String FIELD_MASK =
            "routes.distanceMeters,routes.duration";

    private final RestClient restClient;

    private final String apiKey;

    private final boolean trafficAware;

    public GoogleRoutesCalculationService(
            RestClient.Builder builder,
            @Value("${yocabs.routing.google.api-key}") String apiKey,
            @Value("${yocabs.routing.google.traffic-aware:true}") boolean trafficAware
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "yocabs.routing.google.api-key is required when "
                            + "yocabs.routing.provider=google"
            );
        }

        this.apiKey = apiKey;
        this.trafficAware = trafficAware;
        this.restClient = builder.baseUrl(ENDPOINT).build();
    }

    @Override
    public RouteCalculation calculate(Itinerary itinerary) {

        List<Location> points =
                HaversineRouteCalculationService.RoutePoints.of(itinerary);

        ComputeRoutesRequest request =
                new ComputeRoutesRequest(
                        waypoint(points.getFirst()),
                        waypoint(points.getLast()),
                        points.size() > 2
                                ? points.subList(1, points.size() - 1)
                                .stream()
                                .map(GoogleRoutesCalculationService::waypoint)
                                .toList()
                                : null,
                        "DRIVE",
                        trafficAware
                                ? "TRAFFIC_AWARE"
                                : "TRAFFIC_UNAWARE",
                        "METRIC"
                );

        ComputeRoutesResponse response;
        try {
            response = restClient.post()
                    .uri("/directions/v2:computeRoutes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .body(request)
                    .retrieve()
                    .body(ComputeRoutesResponse.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "Routing provider is unavailable", exception
            );
        }

        if (response == null
                || response.routes() == null
                || response.routes().isEmpty()) {

            throw new IllegalStateException(
                    "Routing provider could not calculate a route"
            );
        }

        GoogleRoute route = response.routes().getFirst();

        return new RouteCalculation(
                BigDecimal.valueOf(route.distanceMeters() / 1000.0)
                        .setScale(2, RoundingMode.HALF_UP),
                Duration.ofSeconds(
                        Math.max(60, parseSeconds(route.duration()))
                )
        );
    }

    private static Waypoint waypoint(Location location) {
        return new Waypoint(
                new WaypointLocation(
                        new LatLng(
                                location.latitude(),
                                location.longitude()
                        )
                )
        );
    }

    /** Routes returns a duration as protobuf seconds, for example "3456s". */
    private static long parseSeconds(String duration) {

        if (duration == null || duration.isBlank()) {
            throw new IllegalStateException(
                    "Routing provider returned no duration"
            );
        }

        String digits =
                duration.endsWith("s")
                        ? duration.substring(0, duration.length() - 1)
                        : duration;

        try {
            return Math.round(Double.parseDouble(digits));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Routing provider returned an unreadable duration: "
                            + duration,
                    exception
            );
        }
    }

    record LatLng(double latitude, double longitude) {
    }

    record WaypointLocation(LatLng latLng) {
    }

    record Waypoint(WaypointLocation location) {
    }

    /* Routes rejects a null "intermediates", so nulls are left out of the body entirely. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ComputeRoutesRequest(
            Waypoint origin,
            Waypoint destination,
            List<Waypoint> intermediates,
            String travelMode,
            String routingPreference,
            String units
    ) {
    }

    record ComputeRoutesResponse(List<GoogleRoute> routes) {
    }

    record GoogleRoute(int distanceMeters, String duration) {
    }
}
