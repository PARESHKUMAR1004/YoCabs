package com.yocabs.api.modules.triprequest.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TripRequestJpaRepository
        extends JpaRepository<TripRequestEntity, UUID> {
}
