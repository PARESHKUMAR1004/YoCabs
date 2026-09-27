package com.yocabs.api.modules.notification.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PushChannelsTest {

    @Test
    void eachKindOfNotificationHasItsOwnSound() {
        assertEquals(PushChannels.BOOKINGS, PushChannels.forType("BOOKING_CONFIRMED"));
        assertEquals(PushChannels.BOOKINGS, PushChannels.forType("BOOKING_RECEIVED"));
        assertEquals(PushChannels.OFFERS, PushChannels.forType("NEGOTIATION_OFFER_RECEIVED"));
        assertEquals(PushChannels.OFFERS, PushChannels.forType("NEGOTIATION_COUNTER_SENT"));
        assertEquals(PushChannels.TRIPS, PushChannels.forType("DRIVER_ASSIGNED"));
        assertEquals(PushChannels.TRIPS, PushChannels.forType("TRIP_STARTED"));
        assertEquals(PushChannels.PAYMENTS, PushChannels.forType("BALANCE_PAID"));
        assertEquals(PushChannels.SUPPORT, PushChannels.forType("SUPPORT_REPLY"));
    }

    @Test
    void thingsGoingWrongUseTheAlertSound() {
        assertEquals(PushChannels.ALERTS, PushChannels.forType("BOOKING_CANCELLED"));
        assertEquals(PushChannels.ALERTS, PushChannels.forType("PAYMENT_FAILED"));
        assertEquals(PushChannels.ALERTS, PushChannels.forType("PAYOUT_REJECTED"));
    }

    @Test
    void anythingElseUsesTheGentleDefault() {
        assertEquals(PushChannels.UPDATES, PushChannels.forType("REVIEW_RECEIVED"));
        assertEquals(PushChannels.UPDATES, PushChannels.forType("SOMETHING_NEW"));
        assertEquals(PushChannels.UPDATES, PushChannels.forType(null));
    }
}
