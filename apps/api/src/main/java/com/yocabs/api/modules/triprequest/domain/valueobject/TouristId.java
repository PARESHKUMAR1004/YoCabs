package com.yocabs.api.modules.triprequest.domain.valueobject;

import java.util.UUID;

public record TouristId(UUID value) {
    public TouristId {
        if (value == null) {
            throw new IllegalArgumentException("Tourist ID cannot be null");
        }
    }
}