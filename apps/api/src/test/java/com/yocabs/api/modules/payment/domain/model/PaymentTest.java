package com.yocabs.api.modules.payment.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentTest {

    private Payment payment() {
        return Payment.initiate(UUID.randomUUID(), PaymentPurpose.TOKEN, new BigDecimal("100.00"), "INR", "sandbox", "order_1");
    }

    @Test
    void aPaymentMustHavePositiveAmountAndAnOrder() {
        assertThrows(IllegalArgumentException.class,
                () -> Payment.initiate(UUID.randomUUID(), PaymentPurpose.TOKEN, BigDecimal.ZERO, "INR", "sandbox", "o"));
        assertThrows(IllegalArgumentException.class,
                () -> Payment.initiate(UUID.randomUUID(), PaymentPurpose.TOKEN, BigDecimal.TEN, "INR", "sandbox", " "));
    }

    @Test
    void itCanSucceedOnlyOnce() {
        Payment payment = payment();

        payment.markSucceeded("pay_1");

        assertTrue(payment.isPaid());
        assertThrows(IllegalStateException.class, () -> payment.markSucceeded("pay_2"));
        assertThrows(IllegalStateException.class, payment::markFailed);
    }

    @Test
    void aFailedPaymentIsNotPaidAndCannotBeRefunded() {
        Payment payment = payment();

        payment.markFailed();

        assertFalse(payment.isPaid());
        assertThrows(IllegalStateException.class, () -> payment.refund(BigDecimal.ONE));
    }

    @Test
    void refundsAccumulateUpToTheCapturedAmount() {
        Payment payment = payment();
        payment.markSucceeded("pay_1");

        payment.refund(new BigDecimal("40.00"));
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, payment.getStatus());
        assertEquals(0, new BigDecimal("60.00").compareTo(payment.refundableAmount()));

        assertThrows(IllegalArgumentException.class, () -> payment.refund(new BigDecimal("60.01")));

        payment.refund(new BigDecimal("60.00"));
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertThrows(IllegalStateException.class, () -> payment.refund(BigDecimal.ONE));
    }

    @Test
    void anUnpaidPaymentCannotBeRefunded() {
        assertThrows(IllegalStateException.class, () -> payment().refund(BigDecimal.ONE));
    }
}
