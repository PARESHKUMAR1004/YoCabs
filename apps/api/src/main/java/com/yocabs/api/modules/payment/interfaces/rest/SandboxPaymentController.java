package com.yocabs.api.modules.payment.interfaces.rest;

import com.yocabs.api.modules.payment.application.PaymentService;
import com.yocabs.api.modules.payment.domain.model.Payment;
import com.yocabs.api.modules.payment.infrastructure.SandboxPaymentGateway;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Development aid: plays the role of the payment provider by sending a
 * correctly signed webhook for a payment. Not available in production.
 */
@RestController
@Profile("!prod")
@ConditionalOnProperty(name = "yocabs.payment.gateway", havingValue = "sandbox", matchIfMissing = true)
@RequestMapping("/api/v1/dev/payments")
public class SandboxPaymentController {

    private final PaymentService paymentService;
    private final SandboxPaymentGateway gateway;

    public SandboxPaymentController(PaymentService paymentService, SandboxPaymentGateway gateway) {
        this.paymentService = paymentService;
        this.gateway = gateway;
    }

    @PostMapping("/{paymentId}/simulate")
    public void simulate(
            @CurrentActor Actor actor,
            @PathVariable UUID paymentId,
            @RequestBody SimulateRequest request
    ) {
        Payment payment = paymentService.get(paymentId);

        boolean success = !"FAILURE".equalsIgnoreCase(request.outcome());

        String payload =
                "{\"eventId\":\"evt_" + UUID.randomUUID() + "\","
                        + "\"type\":\"" + (success ? "PAYMENT_SUCCEEDED" : "PAYMENT_FAILED") + "\","
                        + "\"orderId\":\"" + payment.getGatewayOrderId() + "\","
                        + "\"paymentId\":\"pay_" + UUID.randomUUID().toString().replace("-", "") + "\","
                        + "\"amount\":\"" + payment.getAmount().toPlainString() + "\"}";

        paymentService.handleWebhook(payload, gateway.sign(payload));
    }

    public record SimulateRequest(String outcome) {
    }
}
