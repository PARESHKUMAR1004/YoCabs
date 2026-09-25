package com.yocabs.api.modules.vehicle.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacilityJpaRepository
        extends JpaRepository<FacilityEntity, String> {

    List<FacilityEntity> findByActiveTrueOrderByNameAsc();

    List<FacilityEntity> findAllByOrderByNameAsc();
}
