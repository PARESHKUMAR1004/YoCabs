ALTER TABLE trip_requests
    ADD COLUMN vehicle_category VARCHAR(30);

ALTER TABLE trip_requests
    ADD CONSTRAINT chk_trip_requests_vehicle_category
        CHECK (
            vehicle_category IS NULL
                OR vehicle_category IN (
                                        'SEDAN',
                                        'SUV',
                                        'MUV',
                                        'TEMPO_TRAVELLER',
                                        'BUS'
                )
            );

CREATE INDEX idx_trip_requests_vehicle_category
    ON trip_requests(vehicle_category);