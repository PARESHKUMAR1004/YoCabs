package com.yocabs.api.modules.travelpartner.application.eligibility;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;

public interface TravelPartnerEligibilityRule {

    boolean isEligible(
            TravelPartner travelPartner,
            TripSearchCriteria criteria
    );
}