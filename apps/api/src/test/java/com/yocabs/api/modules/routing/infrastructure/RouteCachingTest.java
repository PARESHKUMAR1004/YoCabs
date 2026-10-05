package com.yocabs.api.modules.routing.infrastructure;

import com.yocabs.api.shared.cache.CacheConfig;
import com.yocabs.api.modules.routing.application.RouteCalculationService;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Proves the @Cacheable wiring actually works through a real Spring AOP proxy: the annotation and
 * its SpEL key expression are never exercised by GoogleRoutesCalculationServiceTest, since that
 * test builds the service with plain `new` and never goes through a cache-aware proxy.
 */
class RouteCachingTest {

    private static final String URL = "https://routes.googleapis.com/directions/v2:computeRoutes";
    private static final Location BBSR = new Location("Bhubaneswar", 20.2961, 85.8245);
    private static final Location PURI = new Location("Puri", 19.8135, 85.8312);

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void closeContext() {
        if (context != null) context.close();
    }

    @Configuration(proxyBeanMethods = false)
    @org.springframework.context.annotation.Import(CacheConfig.class)
    static class Config {
        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }

        @Bean
        @org.springframework.context.annotation.Lazy
        GoogleRoutesCalculationService googleRoutesCalculationService(RestClient.Builder builder) {
            return new GoogleRoutesCalculationService(builder, "test-key", true);
        }
    }

    @Test
    void secondCallForTheSamePhysicalRouteNeverHitsTheProvider() {
        context = new AnnotationConfigApplicationContext(Config.class);

        RestClient.Builder liveBuilder = context.getBean(RestClient.Builder.class);
        MockRestServiceServer mock = MockRestServiceServer.bindTo(liveBuilder).build();
        mock.expect(requestTo(URL))
                .andRespond(withSuccess(
                        "{\"routes\":[{\"distanceMeters\":60200,\"duration\":\"5400s\"}]}",
                        MediaType.APPLICATION_JSON));

        // Looked up by interface: @EnableCaching proxies an interface-implementing bean with a
        // JDK dynamic proxy, which isn't assignable to the concrete class.
        RouteCalculationService service = context.getBean(RouteCalculationService.class);

        var first = service.calculate(new Itinerary(BBSR, List.of(), PURI));
        // Same coordinates, a differently-worded description - still the same physical route.
        var second = service.calculate(new Itinerary(
                new Location("BBSR", 20.2961, 85.8245), List.of(),
                new Location("Puri town", 19.8135, 85.8312)
        ));

        assertEquals(first, second);
        mock.verify(); // fails if a second HTTP call was made, since only one response was queued
    }
}
