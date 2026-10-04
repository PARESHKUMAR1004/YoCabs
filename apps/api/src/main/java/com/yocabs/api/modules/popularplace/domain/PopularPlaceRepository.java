package com.yocabs.api.modules.popularplace.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PopularPlaceRepository {

    List<PopularPlace> findAllOrdered();

    Optional<PopularPlace> findById(UUID id);

    PopularPlace save(PopularPlace place);

    void deleteById(UUID id);
}
