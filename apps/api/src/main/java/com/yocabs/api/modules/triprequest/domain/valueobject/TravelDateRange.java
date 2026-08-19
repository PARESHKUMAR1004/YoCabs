package com.yocabs.api.modules.triprequest.domain.valueobject;

import java.time.LocalDate;

public record TravelDateRange(
        LocalDate startDate,
        LocalDate endDate
) {
    public TravelDateRange {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Travel dates cannot be null"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }
    }
}
