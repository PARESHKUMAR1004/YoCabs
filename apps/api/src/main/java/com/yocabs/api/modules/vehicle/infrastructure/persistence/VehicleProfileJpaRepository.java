package com.yocabs.api.modules.vehicle.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VehicleProfileJpaRepository
        extends JpaRepository<VehicleProfileEntity, UUID> {
}
