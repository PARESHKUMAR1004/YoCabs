package com.yocabs.api.modules.driver.infrastructure.persistence;

import com.yocabs.api.modules.driver.domain.model.Driver;
import com.yocabs.api.modules.driver.domain.repository.DriverRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DriverRepositoryAdapter implements DriverRepository {

    private final DriverJpaRepository jpa;

    public DriverRepositoryAdapter(DriverJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Driver create(Driver driver) {
        return jpa.saveAndFlush(DriverEntity.fromDomain(driver)).toDomain();
    }

    @Override
    @Transactional
    public Driver update(Driver driver) {
        DriverEntity entity =
                jpa.findById(driver.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException("Driver not found: " + driver.getId()));

        entity.updateFromDomain(driver);
        jpa.flush();
        return entity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Driver> findById(UUID id) {
        return jpa.findById(id).map(DriverEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Driver> findByTravelPartnerId(UUID travelPartnerId) {
        return jpa.findByTravelPartnerIdOrderByNameAsc(travelPartnerId).stream()
                .map(DriverEntity::toDomain).toList();
    }
}
