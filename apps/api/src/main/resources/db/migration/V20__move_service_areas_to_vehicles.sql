-- Service areas move from the travel partner to the individual vehicle, so a partner can cover
-- different ground with different cars. Every existing partner area is copied onto each of that
-- partner's vehicles, which keeps current partners matching exactly the trips they matched before.

CREATE TABLE vehicle_service_areas (
    id UUID PRIMARY KEY,

    vehicle_id UUID NOT NULL,

    name VARCHAR(200) NOT NULL,

    latitude DOUBLE PRECISION NOT NULL,

    longitude DOUBLE PRECISION NOT NULL,

    radius_km NUMERIC(10, 2) NOT NULL,

    CONSTRAINT fk_vehicle_service_area_vehicle
        FOREIGN KEY (vehicle_id)
            REFERENCES vehicles(id)
            ON DELETE CASCADE,

    CONSTRAINT chk_vehicle_service_area_latitude
        CHECK (latitude >= -90 AND latitude <= 90),

    CONSTRAINT chk_vehicle_service_area_longitude
        CHECK (longitude >= -180 AND longitude <= 180),

    CONSTRAINT chk_vehicle_service_area_radius
        CHECK (radius_km > 0)
);

CREATE INDEX idx_vehicle_service_areas_vehicle
    ON vehicle_service_areas(vehicle_id);

INSERT INTO vehicle_service_areas (
    id, vehicle_id, name, latitude, longitude, radius_km
)
SELECT
    gen_random_uuid(),
    v.id,
    a.name,
    a.latitude,
    a.longitude,
    a.radius_km
FROM service_areas a
         JOIN vehicles v
              ON v.travel_partner_id = a.travel_partner_id;

DROP TABLE service_areas;
