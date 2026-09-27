package com.yocabs.api.modules.tracking.application.service;

import com.yocabs.api.modules.booking.application.DriverLocationGate;
import com.yocabs.api.modules.tracking.domain.repository.TripLocationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class TripLocationGate implements DriverLocationGate {

    private final TripLocationRepository locations;

    public TripLocationGate(TripLocationRepository locations) {
        this.locations = locations;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRecentLocation(UUID bookingId, Duration within) {
        Instant since = Instant.now().minus(within);

        return locations.findByBookingId(bookingId)
                .filter(location -> location.recordedAt().isAfter(since))
                .isPresent();
    }
}
