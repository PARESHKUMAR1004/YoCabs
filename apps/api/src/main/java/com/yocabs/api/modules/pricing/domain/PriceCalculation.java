package com.yocabs.api.modules.pricing.domain;

import java.math.BigDecimal;
import java.util.List;

public record PriceCalculation(
        BigDecimal totalAmount,
        String currency,
        List<PriceComponent> components
) {

    public PriceCalculation {

        if (totalAmount == null
                || totalAmount.signum() < 0) {

            throw new IllegalArgumentException(
                    "Total amount cannot be negative"
            );
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException(
                    "Currency is required"
            );
        }

        if (components == null) {
            throw new IllegalArgumentException(
                    "Price components are required"
            );
        }

        components = List.copyOf(components);
    }
}