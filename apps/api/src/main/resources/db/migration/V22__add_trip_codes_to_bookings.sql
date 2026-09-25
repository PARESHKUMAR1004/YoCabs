-- One-time codes the tourist reads out to the driver: one to start the trip, one to complete it.
ALTER TABLE bookings
    ADD COLUMN start_code      VARCHAR(6),
    ADD COLUMN completion_code VARCHAR(6);

UPDATE bookings
SET start_code      = lpad((floor(random() * 1000000))::int::text, 6, '0'),
    completion_code = lpad((floor(random() * 1000000))::int::text, 6, '0');

ALTER TABLE bookings
    ALTER COLUMN start_code SET NOT NULL,
    ALTER COLUMN completion_code SET NOT NULL;
