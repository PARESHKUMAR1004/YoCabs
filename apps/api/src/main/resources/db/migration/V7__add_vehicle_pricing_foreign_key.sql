ALTER TABLE vehicle_pricing_configurations
    ADD CONSTRAINT fk_vehicle_pricing_vehicle
        FOREIGN KEY (vehicle_id)
            REFERENCES vehicles(id)
            ON DELETE RESTRICT;