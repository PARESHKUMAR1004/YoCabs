package com.yocabs.api.modules.travelpartner.application.service;

import com.yocabs.api.modules.travelpartner.application.eligibility.TravelPartnerEligibilityRule;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultTravelPartnerEligibilityService
        implements TravelPartnerEligibilityService {

    private final List<TravelPartnerEligibilityRule> rules;

    public DefaultTravelPartnerEligibilityService(
            List<TravelPartnerEligibilityRule> rules
    ) {
        this.rules = rules;
    }

    @Override
    public List<TravelPartner> findEligiblePartners(
            TripRequest tripRequest,
            List<TravelPartner> candidates
    ) {

        return candidates.stream()
                .filter(partner ->
                        rules.stream()
                                .allMatch(rule ->
                                        rule.isEligible(
                                                partner,
                                                tripRequest
                                        )
                                )
                )
                .toList();
    }
}