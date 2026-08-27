package com.yocabs.api.modules.travelpartner.application.service;

import com.yocabs.api.modules.travelpartner.application.command.RemoveServiceAreaCommand;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemoveServiceAreaService {

    private final TravelPartnerRepository travelPartnerRepository;

    public RemoveServiceAreaService(
            TravelPartnerRepository travelPartnerRepository
    ) {
        this.travelPartnerRepository = travelPartnerRepository;
    }

    @Transactional
    public TravelPartner execute(
            RemoveServiceAreaCommand command
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

        travelPartner.removeServiceArea(
                command.serviceAreaId()
        );

        return travelPartnerRepository.update(
                travelPartner
        );
    }
}