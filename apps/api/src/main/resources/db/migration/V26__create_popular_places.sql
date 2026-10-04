-- Destinations shown as quick picks on the tourist home screen, admin-curated with a pin on a
-- map rather than hard-coded in the app, so YoCabs can add or retire a place without a release.
CREATE TABLE popular_places (
    id             UUID PRIMARY KEY,
    name           VARCHAR(120) NOT NULL,
    subtitle       VARCHAR(120),
    latitude       DOUBLE PRECISION NOT NULL,
    longitude      DOUBLE PRECISION NOT NULL,
    display_order  INTEGER NOT NULL DEFAULT 0,
    updated_by     UUID,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_popular_places_display_order ON popular_places (display_order);

-- Seeded with the places the app already showed as a static list, so nothing disappears from the
-- home screen the moment this ships; an admin can edit or remove any of these from here on.
INSERT INTO popular_places (id, name, subtitle, latitude, longitude, display_order, updated_at) VALUES
    (gen_random_uuid(), 'Bhubaneswar Airport', 'Biju Patnaik International Airport', 20.2444, 85.8178, 0, now()),
    (gen_random_uuid(), 'Bhubaneswar Railway Station', 'Bhubaneswar', 20.2695, 85.8436, 1, now()),
    (gen_random_uuid(), 'Puri', 'Odisha', 19.8135, 85.8312, 2, now()),
    (gen_random_uuid(), 'Konark Sun Temple', 'Konark', 19.8876, 86.0945, 3, now()),
    (gen_random_uuid(), 'Chilika Lake (Satapada)', 'Puri district', 19.67, 85.45, 4, now()),
    (gen_random_uuid(), 'Cuttack', 'Odisha', 20.4625, 85.883, 5, now());
