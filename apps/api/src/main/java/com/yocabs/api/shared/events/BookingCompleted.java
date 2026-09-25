package com.yocabs.api.shared.events;

import java.math.BigDecimal;
import java.util.UUID;

/** Published when a trip is completed; drives settlement and review prompts. */
public record BookingCompleted(
        UUID bookingId,
        UUID travelPartnerId,
        UUID touristId,
        String currency,
        BigDecimal totalAmount,
        BigDecimal tokenAmount,
        BigDecimal commissionAmount
) {
}
