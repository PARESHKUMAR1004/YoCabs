CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    trip_request_id UUID NOT NULL,
    tourist_id UUID NOT NULL,
    travel_partner_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    negotiation_id UUID,
    trip_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    passenger_count INTEGER NOT NULL,
    pickup_description VARCHAR(500) NOT NULL,
    destination_description VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    token_amount NUMERIC(12, 2) NOT NULL,
    commission_amount NUMERIC(12, 2) NOT NULL,
    hold_expires_at TIMESTAMP WITH TIME ZONE,
    driver_id UUID,
    idempotency_key VARCHAR(100) NOT NULL,
    cancellation_reason VARCHAR(500),
    cancelled_by_role VARCHAR(30),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_booking_trip_request
        FOREIGN KEY (trip_request_id) REFERENCES trip_requests(id),
    CONSTRAINT fk_booking_partner
        FOREIGN KEY (travel_partner_id) REFERENCES travel_partners(id),
    CONSTRAINT fk_booking_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_booking_negotiation
        FOREIGN KEY (negotiation_id) REFERENCES negotiations(id),

    CONSTRAINT uq_booking_idempotency UNIQUE (tourist_id, idempotency_key),

    CONSTRAINT chk_booking_status CHECK (
        status IN ('PENDING_PAYMENT', 'CONFIRMED', 'IN_PROGRESS',
                   'COMPLETED', 'CANCELLED', 'EXPIRED')
    ),
    CONSTRAINT chk_booking_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_booking_amounts CHECK (
        total_amount >= 0 AND token_amount >= 0 AND commission_amount >= 0
        AND token_amount <= total_amount AND commission_amount <= total_amount
    )
);

-- Only one live booking may exist for a trip request.
CREATE UNIQUE INDEX uq_booking_live_per_trip_request
    ON bookings(trip_request_id)
    WHERE status IN ('PENDING_PAYMENT', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED');

CREATE INDEX idx_bookings_tourist ON bookings(tourist_id);
CREATE INDEX idx_bookings_partner ON bookings(travel_partner_id);
CREATE INDEX idx_bookings_vehicle_dates ON bookings(vehicle_id, start_date, end_date);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_hold_expiry ON bookings(hold_expires_at)
    WHERE status = 'PENDING_PAYMENT';

CREATE TABLE booking_price_components (
    booking_id UUID NOT NULL,
    position INTEGER NOT NULL,
    code VARCHAR(60) NOT NULL,
    description VARCHAR(300) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,

    PRIMARY KEY (booking_id, position),
    CONSTRAINT fk_booking_price_component_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);
