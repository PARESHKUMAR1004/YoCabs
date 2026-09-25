package com.yocabs.api.modules.driver.domain.repository;

import com.yocabs.api.modules.driver.domain.model.Driver;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverRepository {

    Driver create(Driver driver);

    Driver update(Driver driver);

    Optional<Driver> findById(UUID id);

    List<Driver> findByTravelPartnerId(UUID travelPartnerId);
}
