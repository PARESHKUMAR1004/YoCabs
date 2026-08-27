package com.yocabs.api.modules.travelpartner.application.eligibility;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;

public interface TravelPartnerEligibilityRule {

    boolean isEligible(
            TravelPartner travelPartner,
            TripRequest tripRequest
    );
}