CREATE TABLE negotiations (
    id UUID PRIMARY KEY,
    trip_request_id UUID NOT NULL,
    tourist_id UUID NOT NULL,
    travel_partner_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    trip_type VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    listed_amount NUMERIC(12, 2) NOT NULL,
    offered_amount NUMERIC(12, 2) NOT NULL,
    counter_amount NUMERIC(12, 2),
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_negotiation_trip_request
        FOREIGN KEY (trip_request_id) REFERENCES trip_requests(id),
    CONSTRAINT fk_negotiation_partner
        FOREIGN KEY (travel_partner_id) REFERENCES travel_partners(id),
    CONSTRAINT fk_negotiation_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),

    -- MVP rule: one offer (and at most one counter) per partner per trip request.
    CONSTRAINT uq_negotiation_trip_request_partner
        UNIQUE (trip_request_id, travel_partner_id),

    CONSTRAINT chk_negotiation_status CHECK (
        status IN ('OFFER_SENT', 'ACCEPTED', 'REJECTED', 'COUNTER_SENT',
                   'COUNTER_ACCEPTED', 'COUNTER_REJECTED', 'EXPIRED')
    ),
    CONSTRAINT chk_negotiation_amounts CHECK (
        listed_amount > 0
        AND offered_amount > 0
        AND offered_amount < listed_amount
        AND (counter_amount IS NULL
             OR (counter_amount > offered_amount AND counter_amount < listed_amount))
    )
);

CREATE INDEX idx_negotiations_tourist ON negotiations(tourist_id);
CREATE INDEX idx_negotiations_partner_status ON negotiations(travel_partner_id, status);
CREATE INDEX idx_negotiations_expiry ON negotiations(expires_at)
    WHERE status IN ('OFFER_SENT', 'COUNTER_SENT');
