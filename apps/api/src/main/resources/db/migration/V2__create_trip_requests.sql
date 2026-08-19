CREATE TABLE trip_requests (
                               id UUID PRIMARY KEY,

                               tourist_id UUID NOT NULL,

                               status VARCHAR(30) NOT NULL,

                               start_date DATE NOT NULL,
                               end_date DATE NOT NULL,

                               passenger_count INTEGER NOT NULL,

                               trip_brief TEXT NOT NULL,

                               pickup_description VARCHAR(500) NOT NULL,
                               pickup_latitude DOUBLE PRECISION,
                               pickup_longitude DOUBLE PRECISION,

                               destination_description VARCHAR(500) NOT NULL,
                               destination_latitude DOUBLE PRECISION,
                               destination_longitude DOUBLE PRECISION,

                               version BIGINT NOT NULL DEFAULT 0,

                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                               CONSTRAINT chk_trip_request_dates
                                   CHECK (end_date >= start_date),

                               CONSTRAINT chk_trip_request_passengers
                                   CHECK (passenger_count > 0),

                               CONSTRAINT chk_trip_request_version
                                   CHECK (version >= 0)
);

CREATE TABLE trip_request_stops (
                                    id UUID PRIMARY KEY,

                                    trip_request_id UUID NOT NULL,

                                    stop_order INTEGER NOT NULL,

                                    description VARCHAR(500) NOT NULL,

                                    latitude DOUBLE PRECISION,
                                    longitude DOUBLE PRECISION,

                                    CONSTRAINT fk_trip_request_stops_trip_request
                                        FOREIGN KEY (trip_request_id)
                                            REFERENCES trip_requests(id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT uq_trip_request_stop_order
                                        UNIQUE (trip_request_id, stop_order),

                                    CONSTRAINT chk_trip_request_stop_order
                                        CHECK (stop_order >= 0)
);

CREATE INDEX idx_trip_requests_tourist_id
    ON trip_requests(tourist_id);

CREATE INDEX idx_trip_requests_status
    ON trip_requests(status);

CREATE INDEX idx_trip_request_stops_trip_request_id
    ON trip_request_stops(trip_request_id);