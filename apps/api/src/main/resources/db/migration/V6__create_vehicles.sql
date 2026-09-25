CREATE TABLE vehicles (
                          id UUID PRIMARY KEY,

                          travel_partner_id UUID NOT NULL,

                          registration_number VARCHAR(30) NOT NULL,

                          make VARCHAR(100) NOT NULL,

                          model VARCHAR(100) NOT NULL,

                          category VARCHAR(30) NOT NULL,

                          passenger_capacity INTEGER NOT NULL,

                          status VARCHAR(30) NOT NULL,

                          created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                          CONSTRAINT fk_vehicle_travel_partner
                              FOREIGN KEY (travel_partner_id)
                                  REFERENCES travel_partners(id)
                                  ON DELETE RESTRICT,

                          CONSTRAINT uq_vehicle_registration_number
                              UNIQUE (registration_number),

                          CONSTRAINT chk_vehicle_category
                              CHECK (
                                  category IN (
                                               'SEDAN',
                                               'SUV',
                                               'MUV',
                                               'TEMPO_TRAVELLER',
                                               'BUS'
                                      )
                                  ),

                          CONSTRAINT chk_vehicle_status
                              CHECK (
                                  status IN (
                                             'AVAILABLE',
                                             'UNAVAILABLE',
                                             'MAINTENANCE',
                                             'INACTIVE'
                                      )
                                  ),

                          CONSTRAINT chk_vehicle_passenger_capacity
                              CHECK (
                                  passenger_capacity > 0
                                  )
);

CREATE INDEX idx_vehicles_travel_partner
    ON vehicles(travel_partner_id);

CREATE INDEX idx_vehicles_status
    ON vehicles(status);

CREATE INDEX idx_vehicles_category
    ON vehicles(category);