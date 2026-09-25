package com.yocabs.api.modules.travelpartner.infrastructure.persistence;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartnerStatus;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TravelPartnerRepositoryAdapter
        implements TravelPartnerRepository {

    private final TravelPartnerJpaRepository
            travelPartnerJpaRepository;

    public TravelPartnerRepositoryAdapter(
            TravelPartnerJpaRepository travelPartnerJpaRepository
    ) {
        this.travelPartnerJpaRepository =
                travelPartnerJpaRepository;

    }

    @Override
    @Transactional
    public TravelPartner create(
            TravelPartner travelPartner
    ) {

        TravelPartnerEntity entity =
                TravelPartnerEntity.fromDomain(
                        travelPartner
                );

        TravelPartnerEntity saved =
                travelPartnerJpaRepository.save(entity);

        return saved.toDomain();
    }

    @Override
    @Transactional
    public TravelPartner update(
            TravelPartner travelPartner
    ) {

        TravelPartnerEntity entity =
                travelPartnerJpaRepository
                        .findById(travelPartner.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Travel partner not found: "
                                                + travelPartner.getId()
                                )
                        );

        entity.updateFromDomain(
                travelPartner
        );

        return entity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TravelPartner> findById(
            UUID id
    ) {

        return travelPartnerJpaRepository
                .findById(id)
                .map(TravelPartnerEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TravelPartner> findActivePartners() {

        return travelPartnerJpaRepository
                .findByStatus(
                        TravelPartnerStatus.ACTIVE
                )
                .stream()
                .map(TravelPartnerEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TravelPartner> findByStatus(
            TravelPartnerStatus status
    ) {

        return travelPartnerJpaRepository
                .findByStatus(status)
                .stream()
                .map(TravelPartnerEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TravelPartner> findAll() {

        return travelPartnerJpaRepository
                .findAll()
                .stream()
                .map(TravelPartnerEntity::toDomain)
                .toList();
    }

}