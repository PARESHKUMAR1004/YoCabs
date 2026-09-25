-- Where a driver is during a trip. One row per booking, overwritten as the position updates and
-- deleted when the trip ends, so the system never accumulates a history of anyone's movements.

CREATE TABLE trip_locations (
    booking_id UUID PRIMARY KEY,

    driver_id UUID NOT NULL,

    latitude DOUBLE PRECISION NOT NULL,

    longitude DOUBLE PRECISION NOT NULL,

    accuracy_metres DOUBLE PRECISION,

    speed_kph DOUBLE PRECISION,

    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_trip_location_booking
        FOREIGN KEY (booking_id)
            REFERENCES bookings(id)
            ON DELETE CASCADE,

    CONSTRAINT chk_trip_location_latitude
        CHECK (latitude >= -90 AND latitude <= 90),

    CONSTRAINT chk_trip_location_longitude
        CHECK (longitude >= -180 AND longitude <= 180)
);

CREATE INDEX idx_trip_locations_driver
    ON trip_locations(driver_id);
