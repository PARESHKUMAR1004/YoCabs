package com.yocabs.api.modules.triprequest.domain.repository;

import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.valueobject.TripRequestId;

import java.util.Optional;

public interface TripRequestRepository {

    TripRequest create(TripRequest tripRequest);

    TripRequest update(TripRequest tripRequest);

    Optional<TripRequest> findById(TripRequestId id);
}