package com.yocabs.api.modules.travelpartner.domain.repository;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TravelPartnerRepository {

    TravelPartner create(TravelPartner travelPartner);

    TravelPartner update(TravelPartner travelPartner);

    Optional<TravelPartner> findById(UUID id);

    List<TravelPartner> findActivePartners();
}