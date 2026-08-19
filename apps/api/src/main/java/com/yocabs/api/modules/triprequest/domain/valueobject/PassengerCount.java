package com.yocabs.api.modules.triprequest.domain.valueobject;

public record PassengerCount(int value) {
    public PassengerCount {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "Passenger count must be greater than zero"
            );
        }
    }
}
