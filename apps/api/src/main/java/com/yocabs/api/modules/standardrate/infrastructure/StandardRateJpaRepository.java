package com.yocabs.api.modules.standardrate.infrastructure;

import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandardRateJpaRepository extends JpaRepository<StandardRateEntity, VehicleCategory> {
}
