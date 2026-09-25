package com.yocabs.api.modules.payment.domain.model;

/** What a payment is for: the booking token up front, or the rest of the fare after the trip. */
public enum PaymentPurpose {
    TOKEN,
    BALANCE
}
