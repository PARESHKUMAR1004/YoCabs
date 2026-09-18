package com.yocabs.api.modules.triprequest.application.service;

import com.yocabs.api.modules.triprequest.application.command.CreateTripRequestCommand;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.repository.TripRequestRepository;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import com.yocabs.api.modules.triprequest.domain.valueobject.PassengerCount;
import com.yocabs.api.modules.triprequest.domain.valueobject.TouristId;
import com.yocabs.api.modules.triprequest.domain.valueobject.TravelDateRange;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripBrief;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CreateTripRequestService {

    private final TripRequestRepository tripRequestRepository;

    public CreateTripRequestService(
            TripRequestRepository tripRequestRepository
    ) {
        this.tripRequestRepository = tripRequestRepository;
    }

    @Transactional
    public TripRequest execute(
            CreateTripRequestCommand command
    ) {

        Location pickup = new Location(
                command.pickupDescription(),
                command.pickupLatitude(),
                command.pickupLongitude()
        );

        Location destination = new Location(
                command.destinationDescription(),
                command.destinationLatitude(),
                command.destinationLongitude()
        );

        List<Location> stops =
                command.stops() == null
                        ? List.of()
                        : command.stops()
                        .stream()
                        .map(stop -> new Location(
                                stop.description(),
                                stop.latitude(),
                                stop.longitude()
                        ))
                        .toList();

        Itinerary itinerary =
                new Itinerary(
                        pickup,
                        stops,
                        destination
                );

        TravelDateRange travelDateRange =
                new TravelDateRange(
                        command.startDate(),
                        command.endDate()
                );

        PassengerCount passengerCount =
                new PassengerCount(
                        command.passengerCount()
                );

        TripBrief tripBrief =
                new TripBrief(
                        command.tripBrief()
                );

        TripRequest tripRequest =
                TripRequest.create(
                        new TouristId(command.touristId()),
                        itinerary,
                        travelDateRange,
                        passengerCount,
                        tripBrief,
                        command.tripType(),
                        command.vehicleCategory()
                );

        return tripRequestRepository.create(
                tripRequest
        );
    }
}