package com.yocabs.api.modules.driver.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DriverJpaRepository
        extends JpaRepository<DriverEntity, UUID> {

    List<DriverEntity> findByTravelPartnerIdOrderByNameAsc(UUID travelPartnerId);
}
