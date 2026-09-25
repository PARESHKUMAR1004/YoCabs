package com.yocabs.api.shared.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published when a tourist pays the rest of a completed trip's fare online. YoCabs is holding that
 * money on the partner's behalf, so it is owed to the partner.
 */
public record BalancePaid(
        UUID bookingId,
        UUID travelPartnerId,
        UUID touristId,
        String currency,
        BigDecimal amount
) {
}
