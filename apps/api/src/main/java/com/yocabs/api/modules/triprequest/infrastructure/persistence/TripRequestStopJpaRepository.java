package com.yocabs.api.modules.triprequest.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripRequestStopJpaRepository
        extends JpaRepository<TripRequestStopEntity, UUID> {

    List<TripRequestStopEntity> findByTripRequestIdOrderByStopOrder(UUID tripRequestId);
}
