package com.yocabs.api.modules.triprequest.infrastructure.persistence;

import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.repository.TripRequestRepository;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripRequestId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public TripRequest save(TripRequest tripRequest) {

        TripRequestEntity entity =
                TripRequestEntity.from(tripRequest);

        tripRequestJpaRepository.save(entity);

        UUID tripRequestId =
                tripRequest.getId().value();

        List<TripRequestStopEntity> stopEntities =
                new java.util.ArrayList<>();

        var stops = tripRequest.getItinerary().stops();

        for (int i = 0; i < stops.size(); i++) {
            stopEntities.add(
                    TripRequestStopEntity.from(
                            tripRequestId,
                            i,
                            stops.get(i)
                    )
            );
        }

        if (!stopEntities.isEmpty()) {
            tripRequestStopJpaRepository.saveAll(stopEntities);
        }

        return tripRequest;
    }

    @Override
    public Optional<TripRequest> findById(TripRequestId id) {
        throw new UnsupportedOperationException(
                "TripRequest reconstitution is not implemented yet"
        );
    }
}