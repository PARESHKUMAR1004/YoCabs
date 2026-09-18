ALTER TABLE trip_requests
    ADD COLUMN trip_type VARCHAR(50);

ALTER TABLE trip_requests
    ADD CONSTRAINT chk_trip_requests_trip_type
        CHECK (
            trip_type IS NULL
                OR trip_type IN (
                                 'CHAUFFEUR_ONE_WAY',
                                 'CHAUFFEUR_ROUND_TRIP',
                                 'CHAUFFEUR_RENTAL'
                )
            );

CREATE INDEX idx_trip_requests_trip_type
    ON trip_requests(trip_type);