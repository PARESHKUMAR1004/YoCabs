package com.yocabs.api.modules.booking.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class PriceComponentEmbeddable {

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    protected PriceComponentEmbeddable() {
        // JPA
    }

    PriceComponentEmbeddable(String code, String description, BigDecimal amount) {
        this.code = code;
        this.description = description;
        this.amount = amount;
    }

    String getCode() {
        return code;
    }

    String getDescription() {
        return description;
    }

    BigDecimal getAmount() {
        return amount;
    }
}
