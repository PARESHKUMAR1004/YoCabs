package com.yocabs.api.modules.popularplace.infrastructure;

import com.yocabs.api.modules.popularplace.domain.PopularPlace;
import com.yocabs.api.modules.popularplace.domain.PopularPlaceRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PopularPlaceRepositoryAdapter implements PopularPlaceRepository {

    private final PopularPlaceJpaRepository jpa;

    public PopularPlaceRepositoryAdapter(PopularPlaceJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PopularPlace> findAllOrdered() {
        return jpa.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(PopularPlaceEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PopularPlace> findById(UUID id) {
        return jpa.findById(id).map(PopularPlaceEntity::toDomain);
    }

    @Override
    @Transactional
    public PopularPlace save(PopularPlace place) {
        return jpa.saveAndFlush(PopularPlaceEntity.fromDomain(place)).toDomain();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}
