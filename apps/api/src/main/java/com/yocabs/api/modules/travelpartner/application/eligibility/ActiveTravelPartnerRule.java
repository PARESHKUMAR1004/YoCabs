package com.yocabs.api.modules.travelpartner.application.eligibility;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartnerStatus;
import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;
import org.springframework.stereotype.Component;

@Component
public class ActiveTravelPartnerRule
        implements TravelPartnerEligibilityRule {

    @Override
    public boolean isEligible(
            TravelPartner travelPartner,
            TripSearchCriteria criteria
    ) {
        return travelPartner.getStatus()
                == TravelPartnerStatus.ACTIVE;
    }
}