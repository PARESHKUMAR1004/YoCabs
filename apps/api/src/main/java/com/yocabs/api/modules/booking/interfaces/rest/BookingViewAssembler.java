package com.yocabs.api.modules.booking.interfaces.rest;

import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.booking.domain.model.BookingStatus;
import com.yocabs.api.modules.booking.interfaces.rest.BookingController.BookingResponse;
import com.yocabs.api.modules.booking.interfaces.rest.BookingController.PersonSummary;
import com.yocabs.api.modules.booking.interfaces.rest.BookingController.VehicleSummary;
import com.yocabs.api.modules.driver.domain.model.Driver;
import com.yocabs.api.modules.driver.domain.repository.DriverRepository;
import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.modules.review.domain.ReviewRepository;
import com.yocabs.api.modules.identity.domain.repository.UserAccountRepository;
import com.yocabs.api.modules.touristfeedback.application.TouristFeedbackService;
import com.yocabs.api.modules.touristfeedback.domain.TouristFeedbackRepository;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Builds booking responses with the names the apps display. Contact details are
 * shared only where the trip needs them: the tourist sees the assigned driver;
 * partners, admins and the assigned driver see the tourist while the trip is live.
 */
@Component
public class BookingViewAssembler {

    private final TravelPartnerRepository partners;
    private final VehicleRepository vehicles;
    private final DriverRepository drivers;
    private final UserAccountRepository users;
    private final ReviewRepository reviews;
    private final TouristFeedbackService touristFeedbackService;
    private final TouristFeedbackRepository touristFeedback;

    public BookingViewAssembler(
            TravelPartnerRepository partners,
            VehicleRepository vehicles,
            DriverRepository drivers,
            UserAccountRepository users,
            ReviewRepository reviews,
            TouristFeedbackService touristFeedbackService,
            TouristFeedbackRepository touristFeedback
    ) {
        this.partners = partners;
        this.vehicles = vehicles;
        this.drivers = drivers;
        this.users = users;
        this.reviews = reviews;
        this.touristFeedbackService = touristFeedbackService;
        this.touristFeedback = touristFeedback;
    }

    @Transactional(readOnly = true)
    public BookingResponse toResponse(Booking booking, Actor viewer) {
        return toResponses(List.of(booking), viewer).getFirst();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> toResponses(List<Booking> bookings, Actor viewer) {

        Map<UUID, Optional<TravelPartner>> partnerCache = new HashMap<>();
        Map<UUID, Optional<Vehicle>> vehicleCache = new HashMap<>();
        Map<UUID, Optional<Driver>> driverCache = new HashMap<>();
        Map<UUID, Optional<UserAccount>> userCache = new HashMap<>();

        return bookings.stream()
                .map(booking -> {
                    String partnerName =
                            lookup(partnerCache, booking.getTravelPartnerId(), partners::findById)
                                    .map(TravelPartner::getName).orElse(null);

                    VehicleSummary vehicle =
                            lookup(vehicleCache, booking.getVehicleId(), vehicles::findById)
                                    .map(this::summarize).orElse(null);

                    return BookingResponse.from(booking, viewer)
                            .withDetails(
                                    partnerName,
                                    vehicle,
                                    driverFor(booking, viewer, driverCache),
                                    touristFor(booking, viewer, userCache)
                            )
                            .withReviewed(reviewed(booking, viewer))
                            .withDriverFeedbackState(
                                    balanceSettled(booking, viewer), touristRated(booking, viewer));
                })
                .toList();
    }

    /**
     * Whether a completed trip already has a review. Only worth asking for a tourist looking at
     * their own completed trip: that is the only screen offering to rate it.
     */
    private boolean reviewed(Booking booking, Actor viewer) {
        if (viewer.role() != Role.TOURIST || booking.getStatus() != BookingStatus.COMPLETED) {
            return false;
        }
        return reviews.findByBookingId(booking.getId()).isPresent();
    }

    /** Only meaningful for the assigned driver once the trip is done - that is who is asked to wait on it. */
    private boolean balanceSettled(Booking booking, Actor viewer) {
        if (viewer.role() != Role.DRIVER || booking.getStatus() != BookingStatus.COMPLETED) {
            return false;
        }
        return touristFeedbackService.isSettled(booking);
    }

    /** Only meaningful for the assigned driver once the trip is done - that is who can rate it. */
    private boolean touristRated(Booking booking, Actor viewer) {
        if (viewer.role() != Role.DRIVER || booking.getStatus() != BookingStatus.COMPLETED) {
            return false;
        }
        return touristFeedback.findByBookingId(booking.getId()).isPresent();
    }

    private PersonSummary driverFor(
            Booking booking,
            Actor viewer,
            Map<UUID, Optional<Driver>> cache
    ) {
        if (booking.getDriverId() == null || viewer.role() == Role.DRIVER) {
            return null;
        }

        return lookup(cache, booking.getDriverId(), drivers::findById)
                .map(driver -> new PersonSummary(driver.getId(), driver.getName(), driver.getMobile()))
                .orElse(null);
    }

    private PersonSummary touristFor(
            Booking booking,
            Actor viewer,
            Map<UUID, Optional<UserAccount>> cache
    ) {
        if (viewer.role() == Role.TOURIST) {
            return null;
        }

        boolean live = booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.IN_PROGRESS;

        if (viewer.role() == Role.DRIVER && !live) {
            return null;
        }

        return lookup(cache, booking.getTouristId(), users::findById)
                .map(user -> new PersonSummary(user.getId(), user.getDisplayName(), user.getMobile()))
                .orElse(null);
    }

    private VehicleSummary summarize(Vehicle vehicle) {
        return new VehicleSummary(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getCategory().name(),
                vehicle.getPassengerCapacity()
        );
    }

    private static <T> Optional<T> lookup(
            Map<UUID, Optional<T>> cache,
            UUID id,
            Function<UUID, Optional<T>> loader
    ) {
        return cache.computeIfAbsent(id, loader);
    }
}
