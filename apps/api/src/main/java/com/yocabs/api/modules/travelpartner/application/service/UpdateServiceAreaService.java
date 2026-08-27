package com.yocabs.api.modules.travelpartner.application.service;

import com.yocabs.api.modules.travelpartner.application.command.UpdateServiceAreaCommand;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateServiceAreaService {

    private final TravelPartnerRepository travelPartnerRepository;

    public UpdateServiceAreaService(
            TravelPartnerRepository travelPartnerRepository
    ) {
        this.travelPartnerRepository = travelPartnerRepository;
    }

    @Transactional
    public TravelPartner execute(
            UpdateServiceAreaCommand command
    ) {

        TravelPartner travelPartner =
                travelPartnerRepository
                        .findById(command.travelPartnerId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Travel partner not found: "
                                                + command.travelPartnerId()
                                )
                        );

        travelPartner.updateServiceArea(
                command.serviceAreaId(),
                command.name(),
                command.latitude(),
                command.longitude(),
                command.radiusKm()
        );

        return travelPartnerRepository.update(
                travelPartner
        );
    }
}