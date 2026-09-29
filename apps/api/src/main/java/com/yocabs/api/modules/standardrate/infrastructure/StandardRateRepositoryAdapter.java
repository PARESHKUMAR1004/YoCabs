package com.yocabs.api.modules.standardrate.infrastructure;

import com.yocabs.api.modules.standardrate.domain.StandardRate;
import com.yocabs.api.modules.standardrate.domain.StandardRateRepository;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class StandardRateRepositoryAdapter implements StandardRateRepository {

    private final StandardRateJpaRepository jpa;

    public StandardRateRepositoryAdapter(StandardRateJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StandardRate> findAll() {
        return jpa.findAll().stream().map(StandardRateEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<VehicleCategory, StandardRate> findAllByCategory() {
        return findAll().stream().collect(Collectors.toMap(StandardRate::category, Function.identity()));
    }

    @Override
    @Transactional
    public StandardRate upsert(StandardRate rate) {
        return jpa.saveAndFlush(StandardRateEntity.fromDomain(rate)).toDomain();
    }
}
