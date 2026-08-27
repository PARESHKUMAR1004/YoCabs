package com.yocabs.api.modules.travelpartner.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceAreaJpaRepository
        extends JpaRepository<ServiceAreaEntity, UUID> {

    List<ServiceAreaEntity> findByTravelPartnerId(
            UUID travelPartnerId
    );
}
