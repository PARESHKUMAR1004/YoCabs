package com.yocabs.api.modules.triprequest.infrastructure.persistence;

import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;

import java.util.UUID;

public final class TripRequestPersistenceMapper {

    private TripRequestPersistenceMapper() {
    }

    public static TripRequestEntity toEntity(TripRequest tripRequest) {
        TripRequestEntity entity = new TripRequestEntity();

        // We'll need setters/factory methods on the entity.
        // Add those next.
        return entity;
    }
}