-- A reference per-km fare admins set for each vehicle type, so a traveller can tell whether a
-- partner's price is above or below what YoCabs considers typical.
CREATE TABLE standard_rates (
    category      VARCHAR(30) PRIMARY KEY,
    per_km_rate   NUMERIC(10, 2) NOT NULL,
    updated_by    UUID,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_standard_rate_positive CHECK (per_km_rate > 0)
);
