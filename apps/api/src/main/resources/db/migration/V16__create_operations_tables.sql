CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    tourist_id UUID NOT NULL,
    travel_partner_id UUID NOT NULL,
    driver_id UUID,
    rating SMALLINT NOT NULL,
    comment VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_review_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_review_partner FOREIGN KEY (travel_partner_id) REFERENCES travel_partners(id),
    CONSTRAINT uq_review_booking UNIQUE (booking_id),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_partner ON reviews(travel_partner_id);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    recipient_user_id UUID NOT NULL,
    type VARCHAR(60) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    reference_type VARCHAR(40),
    reference_id UUID,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_notification_user
        FOREIGN KEY (recipient_user_id) REFERENCES user_accounts(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_recipient
    ON notifications(recipient_user_id, created_at DESC);

-- Append-only administrative audit trail.
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    actor_role VARCHAR(30) NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(40),
    target_id UUID,
    details VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_target ON audit_logs(target_type, target_id);

-- Append-only partner ledger; the balance is the signed sum of entries.
-- Positive balance: YoCabs owes the partner. Negative: the partner owes YoCabs.
CREATE TABLE wallet_entries (
    id UUID PRIMARY KEY,
    travel_partner_id UUID NOT NULL,
    entry_type VARCHAR(10) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    reference_type VARCHAR(30) NOT NULL,
    reference_id UUID NOT NULL,
    description VARCHAR(300) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_wallet_partner FOREIGN KEY (travel_partner_id) REFERENCES travel_partners(id),
    CONSTRAINT chk_wallet_entry_type CHECK (entry_type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT chk_wallet_amount CHECK (amount > 0),
    -- Each business event posts to the ledger at most once (idempotent settlement).
    CONSTRAINT uq_wallet_reference UNIQUE (reference_type, reference_id, entry_type)
);

CREATE INDEX idx_wallet_entries_partner ON wallet_entries(travel_partner_id, created_at DESC);

CREATE TABLE payouts (
    id UUID PRIMARY KEY,
    travel_partner_id UUID NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_by UUID NOT NULL,
    processed_by UUID,
    bank_reference VARCHAR(100),
    note VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_payout_partner FOREIGN KEY (travel_partner_id) REFERENCES travel_partners(id),
    CONSTRAINT chk_payout_status CHECK (status IN ('REQUESTED', 'PAID', 'REJECTED')),
    CONSTRAINT chk_payout_amount CHECK (amount > 0)
);

CREATE INDEX idx_payouts_partner ON payouts(travel_partner_id, created_at DESC);
CREATE INDEX idx_payouts_status ON payouts(status);
