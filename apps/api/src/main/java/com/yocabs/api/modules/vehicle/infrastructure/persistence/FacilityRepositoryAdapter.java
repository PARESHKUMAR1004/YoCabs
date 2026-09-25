package com.yocabs.api.modules.vehicle.infrastructure.persistence;

import com.yocabs.api.modules.vehicle.domain.model.Facility;
import com.yocabs.api.modules.vehicle.domain.repository.FacilityRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class FacilityRepositoryAdapter implements FacilityRepository {

    private final FacilityJpaRepository jpa;

    public FacilityRepositoryAdapter(FacilityJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Facility save(Facility facility) {
        FacilityEntity entity =
                jpa.findById(facility.code())
                        .orElseGet(() -> FacilityEntity.fromDomain(facility, Instant.now()));

        entity.apply(facility);
        return jpa.saveAndFlush(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Facility> findByCode(String code) {
        return jpa.findById(code).map(FacilityEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Facility> findAll(boolean activeOnly) {
        return (activeOnly ? jpa.findByActiveTrueOrderByNameAsc() : jpa.findAllByOrderByNameAsc())
                .stream().map(FacilityEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Facility> findByCodes(Collection<String> codes) {
        return jpa.findAllById(codes).stream().map(FacilityEntity::toDomain).toList();
    }
}
