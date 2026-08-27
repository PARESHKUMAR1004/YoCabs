package com.yocabs.api.modules.pricing.interfaces.rest.dto;

import com.yocabs.api.modules.triprequest.domain.model.TripType;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdatePricingConfigurationRequest(

        UUID vehicleId,

        TripType tripType,

        BigDecimal baseFee,

        BigDecimal perKmCharge,

        BigDecimal driverAllowance,

        BigDecimal minimumBillableKm,

        Integer includedDurationMinutes,

        BigDecimal includedDistanceKm,

        BigDecimal packagePrice,

        BigDecimal extraHourCharge,

        BigDecimal extraKmCharge
) {
}