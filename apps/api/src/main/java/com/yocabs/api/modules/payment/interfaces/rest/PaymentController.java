package com.yocabs.api.modules.payment.interfaces.rest;

import com.yocabs.api.modules.payment.application.PaymentService;
import com.yocabs.api.modules.payment.domain.model.Payment;
import com.yocabs.api.modules.payment.domain.model.PaymentStatus;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/v1/bookings/{bookingId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse initiate(
            @CurrentActor Actor actor,
            @PathVariable UUID bookingId
    ) {
        return PaymentResponse.from(paymentService.initiate(actor, bookingId));
    }

    @GetMapping("/api/v1/bookings/{bookingId}/payments")
    public List<PaymentResponse> list(
            @CurrentActor Actor actor,
            @PathVariable UUID bookingId
    ) {
        return paymentService.listForBooking(actor, bookingId).stream()
                .map(PaymentResponse::from).toList();
    }

    /** Public endpoint; authenticity comes from the HMAC signature header. */
    @PostMapping("/api/v1/payments/webhook")
    public void webhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Signature", required = false) String signature
    ) {
        paymentService.handleWebhook(payload, signature);
    }

    public record PaymentResponse(
            UUID id,
            UUID bookingId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            String gateway,
            String gatewayOrderId,
            BigDecimal refundedAmount,
            Instant createdAt
    ) {

        static PaymentResponse from(Payment payment) {
            return new PaymentResponse(
                    payment.getId(),
                    payment.getBookingId(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    payment.getStatus(),
                    payment.getGateway(),
                    payment.getGatewayOrderId(),
                    payment.getRefundedAmount(),
                    payment.getCreatedAt()
            );
        }
    }
}
