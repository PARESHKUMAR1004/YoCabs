CREATE TABLE vehicle_pricing_configurations (
                                                id UUID PRIMARY KEY,

                                                vehicle_id UUID NOT NULL,

                                                trip_type VARCHAR(50) NOT NULL,

                                                active BOOLEAN NOT NULL DEFAULT TRUE,

                                                base_fee NUMERIC(12, 2),

                                                per_km_charge NUMERIC(12, 2),

                                                driver_allowance NUMERIC(12, 2),

                                                minimum_billable_km NUMERIC(10, 2),

                                                included_duration_minutes INTEGER,

                                                included_distance_km NUMERIC(10, 2),

                                                package_price NUMERIC(12, 2),

                                                extra_hour_charge NUMERIC(12, 2),

                                                extra_km_charge NUMERIC(12, 2),

                                                created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                                updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                                CONSTRAINT uq_vehicle_pricing_trip_type
                                                    UNIQUE (vehicle_id, trip_type),

                                                CONSTRAINT chk_vehicle_pricing_trip_type
                                                    CHECK (
                                                        trip_type IN (
                                                                      'CHAUFFEUR_ONE_WAY',
                                                                      'CHAUFFEUR_ROUND_TRIP',
                                                                      'CHAUFFEUR_RENTAL'
                                                            )
                                                        ),

                                                CONSTRAINT chk_base_fee_non_negative
                                                    CHECK (
                                                        base_fee IS NULL OR base_fee >= 0
                                                        ),

                                                CONSTRAINT chk_per_km_charge_non_negative
                                                    CHECK (
                                                        per_km_charge IS NULL OR per_km_charge >= 0
                                                        ),

                                                CONSTRAINT chk_driver_allowance_non_negative
                                                    CHECK (
                                                        driver_allowance IS NULL OR driver_allowance >= 0
                                                        ),

                                                CONSTRAINT chk_minimum_billable_km_non_negative
                                                    CHECK (
                                                        minimum_billable_km IS NULL
                                                            OR minimum_billable_km >= 0
                                                        ),

                                                CONSTRAINT chk_included_duration_positive
                                                    CHECK (
                                                        included_duration_minutes IS NULL
                                                            OR included_duration_minutes > 0
                                                        ),

                                                CONSTRAINT chk_included_distance_non_negative
                                                    CHECK (
                                                        included_distance_km IS NULL
                                                            OR included_distance_km >= 0
                                                        ),

                                                CONSTRAINT chk_package_price_non_negative
                                                    CHECK (
                                                        package_price IS NULL OR package_price >= 0
                                                        ),

                                                CONSTRAINT chk_extra_hour_charge_non_negative
                                                    CHECK (
                                                        extra_hour_charge IS NULL
                                                            OR extra_hour_charge >= 0
                                                        ),

                                                CONSTRAINT chk_extra_km_charge_non_negative
                                                    CHECK (
                                                        extra_km_charge IS NULL
                                                            OR extra_km_charge >= 0
                                                        )
);

CREATE INDEX idx_vehicle_pricing_vehicle_id
    ON vehicle_pricing_configurations(vehicle_id);

CREATE INDEX idx_vehicle_pricing_trip_type
    ON vehicle_pricing_configurations(trip_type);