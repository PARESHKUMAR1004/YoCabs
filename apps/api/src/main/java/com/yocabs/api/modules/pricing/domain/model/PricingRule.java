package com.yocabs.api.modules.pricing.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record PricingRule(
        UUID id,
        String code,
        String description,
        PricingRuleType type,
        BigDecimal value,
        String unit,
        boolean active
) {

    public PricingRule {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Pricing rule ID is required"
            );
        }

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Pricing rule code is required"
            );
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Pricing rule description is required"
            );
        }

        if (type == null) {
            throw new IllegalArgumentException(
                    "Pricing rule type is required"
            );
        }

        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(
                    "Pricing rule value cannot be negative"
            );
        }
    }

    public static PricingRule create(
            String code,
            String description,
            PricingRuleType type,
            BigDecimal value,
            String unit
    ) {

        return new PricingRule(
                UUID.randomUUID(),
                code,
                description,
                type,
                value,
                unit,
                true
        );
    }

    public PricingRule deactivate() {

        return new PricingRule(
                id,
                code,
                description,
                type,
                value,
                unit,
                false
        );
    }
}