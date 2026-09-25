package com.yocabs.api.modules.travelpartner.application.service;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;

import java.util.List;

public interface TravelPartnerEligibilityService {

    List<TravelPartner> findEligiblePartners(
            TripSearchCriteria criteria,
            List<TravelPartner> candidates
    );
}