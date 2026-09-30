package com.yocabs.api.modules.invoice.application;

import com.yocabs.api.modules.booking.application.BookingService;
import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.booking.domain.model.BookingStatus;
import com.yocabs.api.modules.booking.domain.repository.BookingRepository;
import com.yocabs.api.modules.identity.domain.model.UserAccount;
import com.yocabs.api.modules.identity.domain.repository.UserAccountRepository;
import com.yocabs.api.modules.payment.domain.model.Payment;
import com.yocabs.api.modules.payment.domain.repository.PaymentRepository;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * The bill for a completed trip: a link the tourist's browser can open with no sign-in (because a
 * browser cannot carry the app's bearer token), and the PDF itself.
 */
@Service
public class InvoiceService {

    private final BookingService bookingService;
    private final BookingRepository bookings;
    private final TravelPartnerRepository partners;
    private final VehicleRepository vehicles;
    private final UserAccountRepository users;
    private final PaymentRepository payments;
    private final InvoiceLinkSigner signer;
    private final InvoicePdfRenderer renderer;
    private final Duration linkValidFor;

    public InvoiceService(
            BookingService bookingService,
            BookingRepository bookings,
            TravelPartnerRepository partners,
            VehicleRepository vehicles,
            UserAccountRepository users,
            PaymentRepository payments,
            InvoiceLinkSigner signer,
            InvoicePdfRenderer renderer,
            @Value("${yocabs.invoice.link-valid-minutes:1440}") long linkValidMinutes
    ) {
        this.bookingService = bookingService;
        this.bookings = bookings;
        this.partners = partners;
        this.vehicles = vehicles;
        this.users = users;
        this.payments = payments;
        this.signer = signer;
        this.renderer = renderer;
        this.linkValidFor = Duration.ofMinutes(linkValidMinutes);
    }

    /** A link valid for a short while, for whoever is allowed to see this booking. */
    @Transactional(readOnly = true)
    public Link createLink(Actor actor, UUID bookingId) {

        Booking booking = bookingService.get(actor, bookingId);

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("An invoice is only available once the trip is completed");
        }

        Instant expiresAt = Instant.now().plus(linkValidFor);
        return new Link(signer.sign(booking.getId(), expiresAt), expiresAt);
    }

    /** Renders the PDF for a valid token; the token itself is the authorisation. */
    @Transactional(readOnly = true)
    public byte[] renderForToken(String token) {

        UUID bookingId = signer.verify(token)
                .orElseThrow(() -> new ResourceNotFoundException("This invoice link has expired"));

        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("An invoice is only available once the trip is completed");
        }

        return render(booking);
    }

    private byte[] render(Booking booking) {

        String partnerName = partners.findById(booking.getTravelPartnerId())
                .map(TravelPartner::getName).orElse(null);

        String vehicleLabel = vehicles.findById(booking.getVehicleId())
                .map(vehicle -> vehicleLabel(vehicle))
                .orElse(null);

        String touristName = users.findById(booking.getTouristId())
                .map(UserAccount::getDisplayName).orElse(null);

        return renderer.render(
                booking, partnerName, vehicleLabel, touristName, payments.findByBookingId(booking.getId())
        );
    }

    private static String vehicleLabel(Vehicle vehicle) {
        return vehicle.getMake() + " " + vehicle.getModel() + " (" + vehicle.getRegistrationNumber() + ")";
    }

    public record Link(String token, Instant expiresAt) {
    }
}
