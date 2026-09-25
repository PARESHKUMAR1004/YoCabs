package com.yocabs.api.modules.vehicle.infrastructure.persistence;

import com.yocabs.api.modules.vehicle.domain.repository.VehicleServiceAreaRepository;
import com.yocabs.api.modules.vehicle.domain.valueobject.ServiceArea;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class VehicleServiceAreaRepositoryAdapter
        implements VehicleServiceAreaRepository {

    private final VehicleServiceAreaJpaRepository jpaRepository;

    public VehicleServiceAreaRepositoryAdapter(
            VehicleServiceAreaJpaRepository jpaRepository
    ) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceArea> findByVehicleId(
            UUID vehicleId
    ) {
        return jpaRepository
                .findByVehicleId(vehicleId)
                .stream()
                .map(VehicleServiceAreaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public ServiceArea add(
            UUID vehicleId,
            ServiceArea serviceArea
    ) {
        return jpaRepository
                .save(
                        VehicleServiceAreaEntity.fromDomain(
                                vehicleId,
                                serviceArea
                        )
                )
                .toDomain();
    }

    @Override
    @Transactional
    public void remove(
            UUID vehicleId,
            UUID serviceAreaId
    ) {
        jpaRepository
                .findById(serviceAreaId)
                .filter(area ->
                        area.getVehicleId().equals(vehicleId)
                )
                .ifPresent(jpaRepository::delete);
    }
}
