package com.yocabs.api.modules.triprequest.domain.valueobject;

// TripBrief.java
public record TripBrief(String value) {
    public TripBrief {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Trip brief cannot be empty");
        }

        value = value.trim();
    }
}