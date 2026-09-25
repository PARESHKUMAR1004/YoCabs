package com.yocabs.api.modules.tripsearch.application;

import com.yocabs.api.modules.pricing.application.model.PricedTravelOption;
import com.yocabs.api.modules.pricing.application.service.TripPricingService;
import com.yocabs.api.modules.pricing.application.service.TripPricingService.PricingOutcome;
import com.yocabs.api.modules.routing.domain.RouteCalculation;
import com.yocabs.api.modules.travelpartner.application.model.EligibleTravelPartner;
import com.yocabs.api.modules.travelpartner.application.service.TripEligibilityService;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.modules.tripsearch.domain.TripSearchCriteria;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TripSearchService {

    private final TravelPartnerRepository
            travelPartnerRepository;

    private final TripEligibilityService
            tripEligibilityService;

    private final TripPricingService
            tripPricingService;

    public TripSearchService(
            TravelPartnerRepository travelPartnerRepository,
            TripEligibilityService tripEligibilityService,
            TripPricingService tripPricingService
    ) {
        this.travelPartnerRepository =
                travelPartnerRepository;

        this.tripEligibilityService =
                tripEligibilityService;

        this.tripPricingService =
                tripPricingService;
    }

    public List<PricedTravelOption> search(
            TripSearchCriteria criteria
    ) {
        return searchWithRoute(criteria).options();
    }

    public TripSearchResult searchWithRoute(
            TripSearchCriteria criteria
    ) {

        if (criteria == null) {
            throw new IllegalArgumentException(
                    "Trip search criteria is required"
            );
        }

        List<TravelPartner> candidates =
                travelPartnerRepository
                        .findActivePartners();

        List<EligibleTravelPartner> eligiblePartners =
                tripEligibilityService
                        .findEligibleOptions(
                                criteria,
                                candidates
                        );

        PricingOutcome outcome =
                tripPricingService
                        .price(
                                criteria,
                                eligiblePartners
                        );

        return new TripSearchResult(
                outcome.route(),
                outcome.options()
        );
    }

    public record TripSearchResult(
            RouteCalculation route,
            List<PricedTravelOption> options
    ) {
    }
}
