CREATE TABLE payments (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    gateway VARCHAR(30) NOT NULL,
    gateway_order_id VARCHAR(100) NOT NULL,
    gateway_payment_id VARCHAR(100),
    refunded_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_payment_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT uq_payment_gateway_order UNIQUE (gateway, gateway_order_id),
    CONSTRAINT chk_payment_status CHECK (
        status IN ('INITIATED', 'SUCCEEDED', 'FAILED', 'PARTIALLY_REFUNDED', 'REFUNDED')
    ),
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_refund CHECK (
        refunded_amount >= 0 AND refunded_amount <= amount
    )
);

-- A booking's token can be successfully paid only once.
CREATE UNIQUE INDEX uq_payment_one_success_per_booking
    ON payments(booking_id)
    WHERE status IN ('SUCCEEDED', 'PARTIALLY_REFUNDED', 'REFUNDED');

CREATE INDEX idx_payments_booking ON payments(booking_id);

-- Append-only ledger of every money movement / gateway event.
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    gateway_event_id VARCHAR(100),
    gateway_reference VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_payment_transaction_payment
        FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT chk_payment_transaction_type CHECK (type IN ('CHARGE', 'REFUND')),
    CONSTRAINT chk_payment_transaction_status CHECK (status IN ('SUCCEEDED', 'FAILED')),
    -- Webhook idempotency: a gateway event is recorded at most once.
    CONSTRAINT uq_payment_transaction_event UNIQUE (gateway_event_id)
);

CREATE INDEX idx_payment_transactions_payment ON payment_transactions(payment_id);
