package com.yocabs.api.modules.popularplace.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PopularPlaceJpaRepository extends JpaRepository<PopularPlaceEntity, UUID> {

    List<PopularPlaceEntity> findAllByOrderByDisplayOrderAscNameAsc();
}
