package com.yocabs.api.modules.travelpartner.infrastructure.persistence;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartnerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TravelPartnerJpaRepository
        extends JpaRepository<TravelPartnerEntity, UUID> {

    List<TravelPartnerEntity> findByStatus(
            TravelPartnerStatus status
    );
}