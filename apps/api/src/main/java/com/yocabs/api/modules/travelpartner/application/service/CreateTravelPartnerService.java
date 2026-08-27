package com.yocabs.api.modules.travelpartner.application.service;

import com.yocabs.api.modules.travelpartner.application.command.CreateTravelPartnerCommand;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateTravelPartnerService {

    private final TravelPartnerRepository travelPartnerRepository;

    public CreateTravelPartnerService(
            TravelPartnerRepository travelPartnerRepository
    ) {
        this.travelPartnerRepository = travelPartnerRepository;
    }

    @Transactional
    public TravelPartner execute(
            CreateTravelPartnerCommand command
    ) {

        TravelPartner travelPartner =
                TravelPartner.create(
                        command.name()
                );

        return travelPartnerRepository.create(
                travelPartner
        );
    }
}