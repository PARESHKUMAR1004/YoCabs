package com.yocabs.api.modules.vehicle.domain.repository;

import com.yocabs.api.modules.vehicle.domain.model.Facility;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FacilityRepository {

    Facility save(Facility facility);

    Optional<Facility> findByCode(String code);

    List<Facility> findAll(boolean activeOnly);

    List<Facility> findByCodes(Collection<String> codes);
}
