package com.yocabs.api.modules.vehicle.infrastructure.persistence;

import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.model.VehicleStatus;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import com.yocabs.api.modules.vehicle.domain.valueobject.ServiceArea;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class VehicleRepositoryAdapter
        implements VehicleRepository {

    private final VehicleJpaRepository
            vehicleJpaRepository;

    private final VehicleServiceAreaJpaRepository
            serviceAreaJpaRepository;

    public VehicleRepositoryAdapter(
            VehicleJpaRepository vehicleJpaRepository,
            VehicleServiceAreaJpaRepository serviceAreaJpaRepository
    ) {
        this.vehicleJpaRepository =
                vehicleJpaRepository;

        this.serviceAreaJpaRepository =
                serviceAreaJpaRepository;
    }

    /** Attaches each vehicle's service areas, fetched for the whole list in one query. */
    private List<Vehicle> withServiceAreas(
            List<VehicleEntity> entities
    ) {

        List<Vehicle> vehicles =
                entities.stream()
                        .map(VehicleEntity::toDomain)
                        .toList();

        if (vehicles.isEmpty()) {
            return vehicles;
        }

        Map<UUID, List<ServiceArea>> areasByVehicle =
                serviceAreaJpaRepository
                        .findByVehicleIdIn(
                                vehicles.stream()
                                        .map(Vehicle::getId)
                                        .toList()
                        )
                        .stream()
                        .collect(Collectors.groupingBy(
                                VehicleServiceAreaEntity::getVehicleId,
                                Collectors.mapping(
                                        VehicleServiceAreaEntity::toDomain,
                                        Collectors.toList()
                                )
                        ));

        vehicles.forEach(vehicle ->
                vehicle.attachServiceAreas(
                        areasByVehicle.getOrDefault(
                                vehicle.getId(),
                                List.of()
                        )
                )
        );

        return vehicles;
    }

    @Override
    @Transactional
    public Vehicle create(
            Vehicle vehicle
    ) {

        VehicleEntity entity =
                VehicleEntity.fromDomain(vehicle);

        VehicleEntity saved =
                vehicleJpaRepository.save(entity);

        return saved.toDomain();
    }

    @Override
    @Transactional
    public Vehicle update(
            Vehicle vehicle
    ) {

        VehicleEntity entity =
                vehicleJpaRepository
                        .findById(vehicle.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Vehicle not found: "
                                                + vehicle.getId()
                                )
                        );

        entity.updateFromDomain(vehicle);

        /*
         * The entity was loaded inside the current
         * transaction, therefore it is managed by
         * Hibernate.
         *
         * No save()/merge() is required here.
         */
        return entity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vehicle> findById(
            UUID id
    ) {

        return vehicleJpaRepository
                .findById(id)
                .map(entity ->
                        withServiceAreas(List.of(entity))
                                .get(0)
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> findByTravelPartnerId(
            UUID travelPartnerId
    ) {

        return withServiceAreas(
                vehicleJpaRepository
                        .findByTravelPartnerId(
                                travelPartnerId
                        )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vehicle> findAvailableVehicles() {

        return withServiceAreas(
                vehicleJpaRepository
                        .findByStatus(
                                VehicleStatus.AVAILABLE
                        )
        );
    }
}