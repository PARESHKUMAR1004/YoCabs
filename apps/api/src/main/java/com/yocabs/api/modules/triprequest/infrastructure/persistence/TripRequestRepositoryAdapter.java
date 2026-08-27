package com.yocabs.api.modules.triprequest.infrastructure.persistence;

import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.model.TripRequestStatus;
import com.yocabs.api.modules.triprequest.domain.repository.TripRequestRepository;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import com.yocabs.api.modules.triprequest.domain.valueobject.PassengerCount;
import com.yocabs.api.modules.triprequest.domain.valueobject.TouristId;
import com.yocabs.api.modules.triprequest.domain.valueobject.TravelDateRange;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripBrief;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripRequestId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class TripRequestRepositoryAdapter
        implements TripRequestRepository {

    private final TripRequestJpaRepository tripRequestJpaRepository;
    private final TripRequestStopJpaRepository tripRequestStopJpaRepository;

    public TripRequestRepositoryAdapter(
            TripRequestJpaRepository tripRequestJpaRepository,
            TripRequestStopJpaRepository tripRequestStopJpaRepository
    ) {
        this.tripRequestJpaRepository = tripRequestJpaRepository;
        this.tripRequestStopJpaRepository = tripRequestStopJpaRepository;
    }

    @Override
    @Transactional
    public TripRequest create(TripRequest tripRequest) {

        TripRequestEntity entity =
                TripRequestEntity.from(tripRequest);

        tripRequestJpaRepository.save(entity);

        saveStops(tripRequest);

        return tripRequest;
    }

    @Override
    @Transactional
    public TripRequest update(TripRequest tripRequest) {

        TripRequestEntity entity =
                tripRequestJpaRepository
                        .findById(tripRequest.getId().value())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Trip request not found: "
                                                + tripRequest.getId().value()
                                )
                        );

        /*
         * The entity returned by findById() is managed by
         * the current Hibernate persistence context.
         *
         * We modify it directly.
         *
         * We DO NOT call repository.save().
         * Hibernate will detect the changes and issue UPDATE
         * statements during transaction commit.
         */
        entity.updateFrom(tripRequest);

        /*
         * Do not touch stops here.
         *
         * The current domain operation may only change
         * status / trip brief. When we introduce an
         * itinerary update operation, we'll implement
         * explicit stop reconciliation there.
         */

        return tripRequest;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TripRequest> findById(
            TripRequestId id
    ) {

        return tripRequestJpaRepository
                .findById(id.value())
                .map(this::toDomain);
    }

    private void saveStops(
            TripRequest tripRequest
    ) {

        var tripRequestId =
                tripRequest.getId().value();

        var stops =
                tripRequest.getItinerary().stops();

        if (stops.isEmpty()) {
            return;
        }

        List<TripRequestStopEntity> stopEntities =
                new java.util.ArrayList<>();

        for (int i = 0; i < stops.size(); i++) {

            stopEntities.add(
                    TripRequestStopEntity.from(
                            tripRequestId,
                            i,
                            stops.get(i)
                    )
            );
        }

        tripRequestStopJpaRepository.saveAll(
                stopEntities
        );
    }

    private TripRequest toDomain(
            TripRequestEntity entity
    ) {

        List<TripRequestStopEntity> stopEntities =
                tripRequestStopJpaRepository
                        .findByTripRequestIdOrderByStopOrder(
                                entity.getId()
                        );

        Location pickup =
                new Location(
                        entity.getPickupDescription(),
                        entity.getPickupLatitude(),
                        entity.getPickupLongitude()
                );

        Location destination =
                new Location(
                        entity.getDestinationDescription(),
                        entity.getDestinationLatitude(),
                        entity.getDestinationLongitude()
                );

        List<Location> stops =
                stopEntities.stream()
                        .map(stop ->
                                new Location(
                                        stop.getDescription(),
                                        stop.getLatitude(),
                                        stop.getLongitude()
                                )
                        )
                        .toList();

        Itinerary itinerary =
                new Itinerary(
                        pickup,
                        stops,
                        destination
                );

        return TripRequest.reconstitute(
                new TripRequestId(entity.getId()),
                new TouristId(entity.getTouristId()),
                itinerary,
                new TravelDateRange(
                        entity.getStartDate(),
                        entity.getEndDate()
                ),
                new PassengerCount(
                        entity.getPassengerCount()
                ),
                new TripBrief(
                        entity.getTripBrief()
                ),
                TripRequestStatus.valueOf(
                        entity.getStatus()
                ),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}