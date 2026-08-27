package com.yocabs.api.modules.travelpartner.infrastructure.persistence;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartnerStatus;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.modules.travelpartner.domain.valueobject.ServiceArea;
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

    private final ServiceAreaJpaRepository
            serviceAreaJpaRepository;

    public TravelPartnerRepositoryAdapter(
            TravelPartnerJpaRepository travelPartnerJpaRepository,
            ServiceAreaJpaRepository serviceAreaJpaRepository
    ) {
        this.travelPartnerJpaRepository =
                travelPartnerJpaRepository;

        this.serviceAreaJpaRepository =
                serviceAreaJpaRepository;
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

        saveServiceAreas(travelPartner);

        return toDomain(saved);
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

        synchronizeServiceAreas(
                travelPartner
        );

        return toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TravelPartner> findById(
            UUID id
    ) {

        return travelPartnerJpaRepository
                .findById(id)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TravelPartner> findActivePartners() {

        return travelPartnerJpaRepository
                .findByStatus(
                        TravelPartnerStatus.ACTIVE
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private void saveServiceAreas(
            TravelPartner travelPartner
    ) {

        List<ServiceAreaEntity> entities =
                travelPartner
                        .getServiceAreas()
                        .stream()
                        .map(serviceArea ->
                                ServiceAreaEntity.fromDomain(
                                        travelPartner.getId(),
                                        serviceArea
                                )
                        )
                        .toList();

        if (!entities.isEmpty()) {
            serviceAreaJpaRepository.saveAll(
                    entities
            );
        }
    }

    private void synchronizeServiceAreas(
            TravelPartner travelPartner
    ) {

        List<ServiceAreaEntity> existing =
                serviceAreaJpaRepository
                        .findByTravelPartnerId(
                                travelPartner.getId()
                        );

        List<ServiceArea> current =
                travelPartner.getServiceAreas();

        /*
         * Delete service areas removed from the
         * domain aggregate.
         */
        for (ServiceAreaEntity existingArea : existing) {

            boolean stillExists =
                    current.stream()
                            .anyMatch(serviceArea ->
                                    serviceArea.id()
                                            .equals(
                                                    existingArea.getId()
                                            )
                            );

            if (!stillExists) {
                serviceAreaJpaRepository.delete(
                        existingArea
                );
            }
        }

        /*
         * Insert new service areas or update existing
         * ones.
         */
        List<ServiceAreaEntity> entities =
                current.stream()
                        .map(serviceArea ->
                                ServiceAreaEntity.fromDomain(
                                        travelPartner.getId(),
                                        serviceArea
                                )
                        )
                        .toList();

        if (!entities.isEmpty()) {
            serviceAreaJpaRepository.saveAll(
                    entities
            );
        }
    }

    private TravelPartner toDomain(
            TravelPartnerEntity entity
    ) {

        List<ServiceAreaEntity> serviceAreas =
                serviceAreaJpaRepository
                        .findByTravelPartnerId(
                                entity.getId()
                        );

        return entity.toDomain(
                serviceAreas
        );
    }
}