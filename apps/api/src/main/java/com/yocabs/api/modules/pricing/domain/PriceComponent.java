package com.yocabs.api.modules.pricing.domain;

import java.math.BigDecimal;

public record PriceComponent(
        String code,
        String description,
        BigDecimal amount
) {

    public PriceComponent {

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Price component code is required"
            );
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Price component description is required"
            );
        }

        if (amount == null) {
            throw new IllegalArgumentException(
                    "Price component amount is required"
            );
        }
    }
}