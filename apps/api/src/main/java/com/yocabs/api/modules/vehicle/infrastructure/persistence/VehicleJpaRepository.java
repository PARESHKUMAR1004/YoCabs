package com.yocabs.api.modules.vehicle.infrastructure.persistence;

import com.yocabs.api.modules.vehicle.domain.model.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleJpaRepository
        extends JpaRepository<VehicleEntity, UUID> {

    List<VehicleEntity> findByTravelPartnerId(
            UUID travelPartnerId
    );

    List<VehicleEntity> findByStatus(
            VehicleStatus status
    );
}