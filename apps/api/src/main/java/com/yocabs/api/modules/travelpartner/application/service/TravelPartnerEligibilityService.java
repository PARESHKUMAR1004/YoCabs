package com.yocabs.api.modules.travelpartner.application.service;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;

import java.util.List;

public interface TravelPartnerEligibilityService {

    List<TravelPartner> findEligiblePartners(
            TripRequest tripRequest,
            List<TravelPartner> candidates
    );
}