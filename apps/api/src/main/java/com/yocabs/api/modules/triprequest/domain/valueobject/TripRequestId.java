package com.yocabs.api.modules.triprequest.domain.valueobject;

import java.util.UUID;

public record TripRequestId(UUID value) {

    public TripRequestId {
        if (value == null) {
            throw new IllegalArgumentException("Trip request ID cannot be null");
        }
    }

    public static TripRequestId generate() {
        return new TripRequestId(UUID.randomUUID());
    }
}
