-- A trip is now completed by the driver on arrival, so only the start code remains.
ALTER TABLE bookings DROP COLUMN completion_code;

-- After the trip the tourist may pay the rest of the fare online. That is a second payment on
-- the same booking, so a booking can now have one successful payment per purpose.
ALTER TABLE payments
    ADD COLUMN purpose VARCHAR(10) NOT NULL DEFAULT 'TOKEN',
    ADD CONSTRAINT chk_payment_purpose CHECK (purpose IN ('TOKEN', 'BALANCE'));

DROP INDEX uq_payment_one_success_per_booking;

CREATE UNIQUE INDEX uq_payment_one_success_per_booking_purpose
    ON payments(booking_id, purpose)
    WHERE status IN ('SUCCEEDED', 'PARTIALLY_REFUNDED', 'REFUNDED');
