package com.yocabs.api.modules.standardrate.domain;

import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;

import java.util.List;
import java.util.Map;

public interface StandardRateRepository {

    List<StandardRate> findAll();

    Map<VehicleCategory, StandardRate> findAllByCategory();

    StandardRate upsert(StandardRate rate);
}
