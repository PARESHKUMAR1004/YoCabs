-- A driver's rating of the traveller on a completed, paid-up trip - the reverse direction of the
-- existing tourist-rates-partner "reviews" table, kept separate rather than merged into it since
-- the subject (a tourist, not a travel partner) and the reviewer (a driver, not a tourist) are
-- both different.
CREATE TABLE tourist_feedback (
    id            UUID PRIMARY KEY,
    booking_id    UUID NOT NULL,
    driver_id     UUID NOT NULL,
    tourist_id    UUID NOT NULL,
    rating        SMALLINT NOT NULL,
    comment       VARCHAR(2000),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_tourist_feedback_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT uq_tourist_feedback_booking UNIQUE (booking_id),
    CONSTRAINT chk_tourist_feedback_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_tourist_feedback_tourist ON tourist_feedback(tourist_id);
