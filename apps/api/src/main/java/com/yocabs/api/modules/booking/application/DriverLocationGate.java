package com.yocabs.api.modules.booking.application;

import java.time.Duration;
import java.util.UUID;

/**
 * Answers one question for booking: is this trip's driver sharing their location right now? It is
 * a port so booking never depends on how tracking stores positions.
 */
public interface DriverLocationGate {

    boolean hasRecentLocation(UUID bookingId, Duration within);
}
