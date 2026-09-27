package com.yocabs.api.modules.notification.application;

/**
 * Which "sound and importance" a notification uses on the phone. The apps create one Android
 * notification channel per name here, each with its own sound, so people can tell a new offer from
 * a warning without looking. Keep the names in step with the app's notification channels.
 */
public final class PushChannels {

    public static final String BOOKINGS = "bookings";
    public static final String OFFERS = "offers";
    public static final String TRIPS = "trips";
    public static final String PAYMENTS = "payments";
    public static final String ALERTS = "alerts";
    public static final String SUPPORT = "support";
    public static final String UPDATES = "updates";

    private PushChannels() {
    }

    public static String forType(String type) {
        if (type == null) {
            return UPDATES;
        }

        return switch (type) {
            case "BOOKING_CONFIRMED", "BOOKING_RECEIVED" -> BOOKINGS;
            case "BOOKING_CANCELLED", "BOOKING_HOLD_EXPIRED", "PAYMENT_FAILED", "PAYOUT_REJECTED" -> ALERTS;
            case "DRIVER_ASSIGNED", "TRIP_ASSIGNED", "TRIP_STARTED", "TRIP_COMPLETED" -> TRIPS;
            case "BALANCE_PAID", "PAYOUT_PAID", "PAYMENT_REFUNDED" -> PAYMENTS;
            default -> {
                if (type.startsWith("NEGOTIATION")) {
                    yield OFFERS;
                }
                yield type.startsWith("SUPPORT") ? SUPPORT : UPDATES;
            }
        };
    }
}
