-- A driver's id is the id of their DRIVER login in user_accounts.
CREATE TABLE drivers (
    id UUID PRIMARY KEY,
    travel_partner_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    mobile VARCHAR(20) NOT NULL,
    license_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_driver_user
        FOREIGN KEY (id) REFERENCES user_accounts(id),
    CONSTRAINT fk_driver_partner
        FOREIGN KEY (travel_partner_id) REFERENCES travel_partners(id),
    CONSTRAINT uq_driver_mobile UNIQUE (mobile),
    CONSTRAINT uq_driver_license UNIQUE (license_number),
    CONSTRAINT chk_driver_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_drivers_partner ON drivers(travel_partner_id);

ALTER TABLE bookings
    ADD CONSTRAINT fk_booking_driver
        FOREIGN KEY (driver_id) REFERENCES drivers(id);
