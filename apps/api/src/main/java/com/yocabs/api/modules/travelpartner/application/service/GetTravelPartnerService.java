package com.yocabs.api.modules.travelpartner.application.service;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetTravelPartnerService {

    private final TravelPartnerRepository travelPartnerRepository;

    public GetTravelPartnerService(TravelPartnerRepository travelPartnerRepository) {
        this.travelPartnerRepository = travelPartnerRepository;
    }

    /** The partner's own organisation (or any, for admins). */
    @Transactional(readOnly = true)
    public TravelPartner execute(Actor actor, UUID travelPartnerId) {

        actor.requirePartnerAccess(travelPartnerId);

        return travelPartnerRepository.findById(travelPartnerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Travel partner not found: " + travelPartnerId));
    }
}
