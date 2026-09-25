package com.yocabs.api.modules.payment.application;

import com.yocabs.api.modules.booking.application.BookingService;
import com.yocabs.api.modules.booking.domain.model.Booking;
import com.yocabs.api.modules.booking.domain.model.BookingStatus;
import com.yocabs.api.modules.payment.application.PaymentGateway.WebhookEvent;
import com.yocabs.api.modules.payment.domain.model.Payment;
import com.yocabs.api.modules.payment.domain.model.PaymentTransaction;
import com.yocabs.api.modules.payment.domain.repository.PaymentRepository;
import com.yocabs.api.shared.events.NotificationRequested;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final BookingService bookingService;
    private final ApplicationEventPublisher events;

    public PaymentService(
            PaymentRepository payments,
            PaymentGateway gateway,
            BookingService bookingService,
            ApplicationEventPublisher events
    ) {
        this.payments = payments;
        this.gateway = gateway;
        this.bookingService = bookingService;
        this.events = events;
    }

    /** Starts (or resumes) the 5% token payment for a booking awaiting payment. */
    @Transactional
    public Payment initiate(Actor actor, UUID bookingId) {

        actor.requireRole(Role.TOURIST);

        Booking booking = bookingService.get(actor, bookingId);

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("This booking is not awaiting payment");
        }

        if (booking.isHoldExpired(Instant.now())) {
            throw new IllegalStateException(
                    "The booking hold has expired. Please start a new booking."
            );
        }

        if (booking.getTokenAmount().signum() <= 0) {
            throw new IllegalStateException("This booking has no payable token amount");
        }

        return payments.findInitiatedByBookingId(bookingId)
                .orElseGet(() -> {
                    String orderId =
                            gateway.createOrder(
                                    booking.getId().toString(),
                                    booking.getTokenAmount(),
                                    booking.getCurrency()
                            );

                    return payments.create(
                            Payment.initiate(
                                    booking.getId(),
                                    booking.getTokenAmount(),
                                    booking.getCurrency(),
                                    gateway.name(),
                                    orderId
                            )
                    );
                });
    }

    /**
     * Idempotent, signature-verified gateway callback. Safe to receive any
     * number of times; every gateway event is applied at most once.
     */
    @Transactional
    public void handleWebhook(String payload, String signature) {

        if (!gateway.verifyWebhookSignature(payload, signature)) {
            throw new BadCredentialsException("Invalid webhook signature");
        }

        WebhookEvent event = gateway.parseWebhook(payload);

        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("Webhook event id is required");
        }

        if (payments.transactionExistsForEvent(event.eventId())) {
            log.info("Ignoring duplicate webhook event {}", event.eventId());
            return;
        }

        Payment payment =
                payments.findByGatewayOrderId(gateway.name(), event.orderId()).orElse(null);

        if (payment == null) {
            log.warn("Webhook {} references unknown order {}", event.eventId(), event.orderId());
            return;
        }

        Booking booking = bookingService.get(systemActor(), payment.getBookingId());

        if (event.type() == WebhookEvent.Type.PAYMENT_FAILED) {
            recordFailure(payment, event, booking);
            return;
        }

        if (event.amount().compareTo(payment.getAmount()) != 0) {
            throw new IllegalArgumentException("Webhook amount does not match the payment");
        }

        if (payment.isPaid()) {
            // Already captured through another event; just remember this one.
            payments.appendTransaction(
                    PaymentTransaction.record(
                            payment.getId(), PaymentTransaction.Type.CHARGE, payment.getAmount(),
                            PaymentTransaction.Outcome.SUCCEEDED, event.eventId(), event.paymentId()
                    )
            );
            return;
        }

        payment.markSucceeded(event.paymentId());
        payments.update(payment);

        payments.appendTransaction(
                PaymentTransaction.record(
                        payment.getId(), PaymentTransaction.Type.CHARGE, payment.getAmount(),
                        PaymentTransaction.Outcome.SUCCEEDED, event.eventId(), event.paymentId()
                )
        );

        boolean confirmed = bookingService.confirmAfterPayment(payment.getBookingId());

        if (!confirmed) {
            // Money was captured for a booking that can no longer be honoured.
            refund(payment.getBookingId(), payment.getAmount(), "booking-unavailable-" + event.eventId());

            events.publishEvent(
                    NotificationRequested.toUser(
                            booking.getTouristId(),
                            "PAYMENT_REFUNDED",
                            "Payment refunded",
                            "The vehicle became unavailable before your payment completed. "
                                    + "Your payment has been refunded in full.",
                            "BOOKING",
                            booking.getId()
                    )
            );
        }
    }

    /** Refunds up to the captured amount for a booking; a no-op when nothing was paid. */
    @Transactional
    public void refund(UUID bookingId, BigDecimal amount, String reason) {

        Payment payment = payments.findPaidByBookingId(bookingId).orElse(null);

        if (payment == null || amount == null || amount.signum() <= 0) {
            return;
        }

        BigDecimal refundAmount = amount.min(payment.refundableAmount());

        if (refundAmount.signum() <= 0) {
            return;
        }

        String reference =
                gateway.refund(
                        payment.getGatewayPaymentId(),
                        refundAmount,
                        payment.getCurrency(),
                        payment.getId() + ":" + payment.getRefundedAmount() + ":" + reason
                );

        payment.refund(refundAmount);
        payments.update(payment);

        payments.appendTransaction(
                PaymentTransaction.record(
                        payment.getId(), PaymentTransaction.Type.REFUND, refundAmount,
                        PaymentTransaction.Outcome.SUCCEEDED, null, reference
                )
        );
    }

    @Transactional(readOnly = true)
    public List<Payment> listForBooking(Actor actor, UUID bookingId) {
        bookingService.get(actor, bookingId);
        return payments.findByBookingId(bookingId);
    }

    @Transactional(readOnly = true)
    public Payment get(UUID paymentId) {
        return payments.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
    }

    private void recordFailure(Payment payment, WebhookEvent event, Booking booking) {

        if (payment.getStatus() == com.yocabs.api.modules.payment.domain.model.PaymentStatus.INITIATED) {
            payment.markFailed();
            payments.update(payment);
        }

        payments.appendTransaction(
                PaymentTransaction.record(
                        payment.getId(), PaymentTransaction.Type.CHARGE, payment.getAmount(),
                        PaymentTransaction.Outcome.FAILED, event.eventId(), event.paymentId()
                )
        );

        events.publishEvent(
                NotificationRequested.toUser(
                        booking.getTouristId(),
                        "PAYMENT_FAILED",
                        "Payment failed",
                        "Your payment did not go through. You can retry before the booking hold expires.",
                        "BOOKING",
                        booking.getId()
                )
        );
    }

    /** Internal caller identity for gateway-initiated work (no end user). */
    private static Actor systemActor() {
        return new Actor(new UUID(0L, 0L), Role.SUPER_ADMIN, null);
    }
}
