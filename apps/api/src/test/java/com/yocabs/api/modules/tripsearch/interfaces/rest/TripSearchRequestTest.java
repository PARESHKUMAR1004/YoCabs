package com.yocabs.api.modules.tripsearch.interfaces.rest;

import com.yocabs.api.modules.triprequest.domain.model.TripType;
import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;
import com.yocabs.api.modules.tripsearch.interfaces.rest.TripSearchController.LocationRequest;
import com.yocabs.api.modules.tripsearch.interfaces.rest.TripSearchController.TripSearchRequest;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TripSearchRequestTest {

    private static final LocationRequest PICKUP =
            new LocationRequest("Bhubaneswar Airport", 20.2444, 85.8178);

    private static final LocationRequest DESTINATION =
            new LocationRequest("Puri", 19.8135, 85.8312);

    @Test
    void shouldMapRequestWithStopsAndOptionalFilters() {

        LocationRequest stop =
                new LocationRequest("Konark", 19.8876, 86.0945);

        TripSearchCriteria criteria =
                request(
                        PICKUP,
                        DESTINATION,
                        List.of(stop),
                        4,
                        VehicleCategory.MUV,
                        TripType.CHAUFFEUR_ONE_WAY
                ).toCriteria();

        assertEquals(1, criteria.itinerary().stops().size());
        assertEquals("Konark", criteria.itinerary().stops().getFirst().description());
        assertEquals(4, criteria.passengerCount());
        assertEquals(VehicleCategory.MUV, criteria.vehicleCategory());
        assertEquals(TripType.CHAUFFEUR_ONE_WAY, criteria.tripType());
    }

    @Test
    void shouldTreatMissingStopsCategoryAndTripTypeAsNoPreference() {

        TripSearchCriteria criteria =
                request(PICKUP, DESTINATION, null, 4, null, null).toCriteria();

        assertEquals(0, criteria.itinerary().stops().size());
        assertNull(criteria.vehicleCategory());
        assertNull(criteria.tripType());
    }

    @Test
    void shouldRejectMissingPickup() {

        assertThrows(
                IllegalArgumentException.class,
                () -> request(null, DESTINATION, null, 4, null, null).toCriteria()
        );
    }

    @Test
    void shouldRejectMissingDestination() {

        assertThrows(
                IllegalArgumentException.class,
                () -> request(PICKUP, null, null, 4, null, null).toCriteria()
        );
    }

    @Test
    void shouldRejectMissingPassengerCount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> request(PICKUP, DESTINATION, null, null, null, null).toCriteria()
        );
    }

    @Test
    void shouldRejectNonPositivePassengerCount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> request(PICKUP, DESTINATION, null, 0, null, null).toCriteria()
        );
    }

    @Test
    void shouldRejectEndDateBeforeStartDate() {

        TripSearchRequest request =
                new TripSearchRequest(
                        PICKUP,
                        DESTINATION,
                        null,
                        LocalDate.of(2026, 8, 30),
                        LocalDate.of(2026, 8, 29),
                        4,
                        null,
                        null
                );

        assertThrows(IllegalArgumentException.class, request::toCriteria);
    }

    @Test
    void shouldNotAcceptClientSuppliedDistanceOrDuration() {

        List<String> fields =
                Arrays.stream(TripSearchRequest.class.getRecordComponents())
                        .map(RecordComponent::getName)
                        .map(String::toLowerCase)
                        .toList();

        assertFalse(fields.stream().anyMatch(name -> name.contains("distance")));
        assertFalse(fields.stream().anyMatch(name -> name.contains("duration")));
    }

    private TripSearchRequest request(
            LocationRequest pickup,
            LocationRequest destination,
            List<LocationRequest> stops,
            Integer passengerCount,
            VehicleCategory category,
            TripType tripType
    ) {

        return new TripSearchRequest(
                pickup,
                destination,
                stops,
                LocalDate.of(2026, 8, 30),
                LocalDate.of(2026, 8, 30),
                passengerCount,
                category,
                tripType
        );
    }
}
