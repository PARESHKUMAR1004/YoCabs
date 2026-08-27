package com.yocabs.api.modules.travelpartner.application.service;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ActivateTravelPartnerService {

    private final TravelPartnerRepository travelPartnerRepository;

    public ActivateTravelPartnerService(
            TravelPartnerRepository travelPartnerRepository
    ) {
        this.travelPartnerRepository = travelPartnerRepository;
    }

    @Transactional
    public TravelPartner execute(UUID id) {

        TravelPartner travelPartner =
                travelPartnerRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Travel partner not found: " + id
                                )
                        );

        travelPartner.activate();

        return travelPartnerRepository.update(
                travelPartner
        );
    }
}