package com.yocabs.api.modules.routing.infrastructure;

import com.yocabs.api.modules.routing.domain.RouteCalculation;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleRoutesCalculationServiceTest {

    private static final Location BBSR = new Location("Bhubaneswar", 20.2961, 85.8245);
    private static final Location KONARK = new Location("Konark", 19.8876, 86.0945);
    private static final Location PURI = new Location("Puri", 19.8135, 85.8312);

    private static final String URL =
            "https://routes.googleapis.com/directions/v2:computeRoutes";

    private MockRestServiceServer server;

    private GoogleRoutesCalculationService serviceReturning(String body) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo(URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "test-key"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        return new GoogleRoutesCalculationService(builder, "test-key", true);
    }

    @Test
    void shouldReadDistanceAndDurationFromTheResponse() {

        GoogleRoutesCalculationService service =
                serviceReturning("{\"routes\":[{\"distanceMeters\":60200,\"duration\":\"5400s\"}]}");

        RouteCalculation route =
                service.calculate(new Itinerary(BBSR, List.of(), PURI));

        assertEquals(0, route.distanceKm().compareTo(new java.math.BigDecimal("60.20")));
        assertEquals(90, route.duration().toMinutes());
        server.verify();
    }

    @Test
    void shouldSendStopsAsIntermediateWaypoints() {

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mock = MockRestServiceServer.bindTo(builder).build();

        mock.expect(requestTo(URL))
                .andExpect(jsonPath("$.origin.location.latLng.latitude").value(20.2961))
                .andExpect(jsonPath("$.destination.location.latLng.latitude").value(19.8135))
                .andExpect(jsonPath("$.intermediates[0].location.latLng.latitude").value(19.8876))
                .andExpect(jsonPath("$.routingPreference").value("TRAFFIC_AWARE"))
                .andRespond(withSuccess(
                        "{\"routes\":[{\"distanceMeters\":95000,\"duration\":\"8100s\"}]}",
                        MediaType.APPLICATION_JSON));

        new GoogleRoutesCalculationService(builder, "test-key", true)
                .calculate(new Itinerary(BBSR, List.of(KONARK), PURI));

        mock.verify();
    }

    @Test
    void shouldLeaveIntermediatesOutWhenThereAreNoStops() {

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mock = MockRestServiceServer.bindTo(builder).build();

        // Routes rejects a null "intermediates", so the field must be absent entirely.
        mock.expect(requestTo(URL))
                .andExpect(jsonPath("$.intermediates").doesNotExist())
                .andRespond(withSuccess(
                        "{\"routes\":[{\"distanceMeters\":60200,\"duration\":\"5400s\"}]}",
                        MediaType.APPLICATION_JSON));

        new GoogleRoutesCalculationService(builder, "test-key", true)
                .calculate(new Itinerary(BBSR, List.of(), PURI));

        mock.verify();
    }

    @Test
    void shouldNeverReturnAZeroDuration() {

        GoogleRoutesCalculationService service =
                serviceReturning("{\"routes\":[{\"distanceMeters\":300,\"duration\":\"5s\"}]}");

        RouteCalculation route =
                service.calculate(new Itinerary(BBSR, List.of(), PURI));

        // RouteCalculation refuses a zero duration, so a very short hop floors at a minute.
        assertEquals(60, route.duration().toSeconds());
    }

    @Test
    void shouldFailLoudlyWhenGoogleReturnsNoRoute() {

        GoogleRoutesCalculationService service =
                serviceReturning("{\"routes\":[]}");

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> service.calculate(new Itinerary(BBSR, List.of(), PURI))
        );

        assertTrue(failure.getMessage().contains("could not calculate"));
    }

    @Test
    void shouldFailLoudlyWhenGoogleIsDown() {

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mock = MockRestServiceServer.bindTo(builder).build();
        mock.expect(requestTo(URL)).andRespond(withServerError());

        GoogleRoutesCalculationService service =
                new GoogleRoutesCalculationService(builder, "test-key", true);

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> service.calculate(new Itinerary(BBSR, List.of(), PURI))
        );

        assertTrue(failure.getMessage().contains("unavailable"));
    }

    @Test
    void shouldRefuseToStartWithoutAnApiKey() {

        assertThrows(
                IllegalStateException.class,
                () -> new GoogleRoutesCalculationService(RestClient.builder(), "  ", true)
        );
    }
}
