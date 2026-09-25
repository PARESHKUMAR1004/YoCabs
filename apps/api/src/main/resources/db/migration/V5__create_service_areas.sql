CREATE TABLE service_areas (
                               id UUID PRIMARY KEY,

                               travel_partner_id UUID NOT NULL,

                               name VARCHAR(200) NOT NULL,

                               latitude DOUBLE PRECISION NOT NULL,

                               longitude DOUBLE PRECISION NOT NULL,

                               radius_km NUMERIC(10, 2) NOT NULL,

                               CONSTRAINT fk_service_area_travel_partner
                                   FOREIGN KEY (travel_partner_id)
                                       REFERENCES travel_partners(id)
                                       ON DELETE CASCADE,

                               CONSTRAINT chk_service_area_latitude
                                   CHECK (
                                       latitude >= -90
                                           AND latitude <= 90
                                       ),

                               CONSTRAINT chk_service_area_longitude
                                   CHECK (
                                       longitude >= -180
                                           AND longitude <= 180
                                       ),

                               CONSTRAINT chk_service_area_radius
                                   CHECK (
                                       radius_km > 0
                                       )
);

CREATE INDEX idx_service_areas_travel_partner
    ON service_areas(travel_partner_id);