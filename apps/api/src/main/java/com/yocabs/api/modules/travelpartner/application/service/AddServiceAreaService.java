package com.yocabs.api.modules.travelpartner.application.service;

import com.yocabs.api.modules.travelpartner.application.command.AddServiceAreaCommand;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.modules.travelpartner.domain.valueobject.ServiceArea;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddServiceAreaService {

    private final TravelPartnerRepository travelPartnerRepository;

    public AddServiceAreaService(
            TravelPartnerRepository travelPartnerRepository
    ) {
        this.travelPartnerRepository = travelPartnerRepository;
    }

    @Transactional
    public TravelPartner execute(
            AddServiceAreaCommand command
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

        ServiceArea serviceArea =
                ServiceArea.create(
                        command.name(),
                        command.latitude(),
                        command.longitude(),
                        command.radiusKm()
                );

        travelPartner.addServiceArea(
                serviceArea
        );

        return travelPartnerRepository.update(
                travelPartner
        );
    }
}