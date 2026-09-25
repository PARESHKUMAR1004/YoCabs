package com.yocabs.api.modules.negotiation.domain.model;

public enum NegotiationStatus {
    OFFER_SENT,
    ACCEPTED,
    REJECTED,
    COUNTER_SENT,
    COUNTER_ACCEPTED,
    COUNTER_REJECTED,
    EXPIRED;

    public boolean isOpen() {
        return this == OFFER_SENT || this == COUNTER_SENT;
    }
}
