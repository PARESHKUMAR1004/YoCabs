-- Facilities are configurable data, not hard-coded booleans.
CREATE TABLE facilities (
    code VARCHAR(40) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT chk_facility_code CHECK (code ~ '^[A-Z][A-Z0-9_]{1,39}$')
);

INSERT INTO facilities (code, name) VALUES
    ('AC', 'Air conditioning'),
    ('HEATER', 'Heater'),
    ('MUSIC_SYSTEM', 'Music system'),
    ('SEAT_COVERS', 'Seat covers'),
    ('SANITIZED_CAB', 'Sanitized cab'),
    ('MOBILE_CHARGER', 'Mobile charger'),
    ('WATER_BOTTLE', 'Water bottle'),
    ('FIRST_AID_KIT', 'First aid kit'),
    ('EXTRA_LUGGAGE_SPACE', 'Extra luggage space'),
    ('TOLL_PARKING_INCLUDED', 'Toll and parking included');

CREATE TABLE vehicle_profiles (
    vehicle_id UUID PRIMARY KEY,
    fuel_type VARCHAR(20),
    transmission VARCHAR(20),
    model_year SMALLINT,
    luggage_capacity SMALLINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_vehicle_profile_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    CONSTRAINT chk_vehicle_fuel_type
        CHECK (fuel_type IS NULL OR fuel_type IN ('PETROL', 'DIESEL', 'CNG', 'ELECTRIC', 'HYBRID')),
    CONSTRAINT chk_vehicle_transmission
        CHECK (transmission IS NULL OR transmission IN ('MANUAL', 'AUTOMATIC')),
    CONSTRAINT chk_vehicle_model_year
        CHECK (model_year IS NULL OR model_year BETWEEN 1990 AND 2100),
    CONSTRAINT chk_vehicle_luggage
        CHECK (luggage_capacity IS NULL OR luggage_capacity BETWEEN 0 AND 50)
);

CREATE TABLE vehicle_facilities (
    vehicle_id UUID NOT NULL,
    facility_code VARCHAR(40) NOT NULL,

    PRIMARY KEY (vehicle_id, facility_code),
    CONSTRAINT fk_vehicle_facility_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    CONSTRAINT fk_vehicle_facility_facility
        FOREIGN KEY (facility_code) REFERENCES facilities(code)
);
