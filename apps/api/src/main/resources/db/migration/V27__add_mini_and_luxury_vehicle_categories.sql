ALTER TABLE vehicles
    DROP CONSTRAINT chk_vehicle_category;

ALTER TABLE vehicles
    ADD CONSTRAINT chk_vehicle_category
        CHECK (
            category IN (
                         'MINI',
                         'SEDAN',
                         'SUV',
                         'MUV',
                         'LUXURY',
                         'TEMPO_TRAVELLER',
                         'BUS'
                )
            );

ALTER TABLE trip_requests
    DROP CONSTRAINT chk_trip_requests_vehicle_category;

ALTER TABLE trip_requests
    ADD CONSTRAINT chk_trip_requests_vehicle_category
        CHECK (
            vehicle_category IS NULL
                OR vehicle_category IN (
                                        'MINI',
                                        'SEDAN',
                                        'SUV',
                                        'MUV',
                                        'LUXURY',
                                        'TEMPO_TRAVELLER',
                                        'BUS'
                )
            );
