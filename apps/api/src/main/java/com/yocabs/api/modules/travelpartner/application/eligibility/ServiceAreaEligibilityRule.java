package com.yocabs.api.modules.travelpartner.application.eligibility;

import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.valueobject.ServiceArea;
import com.yocabs.api.modules.triprequest.domain.model.TripRequest;
import com.yocabs.api.modules.triprequest.domain.valueobject.Location;
import org.springframework.stereotype.Component;

@Component
public class ServiceAreaEligibilityRule
        implements TravelPartnerEligibilityRule {

    @Override
    public boolean isEligible(
            TravelPartner travelPartner,
            TripRequest tripRequest
    ) {

        Location pickup =
                tripRequest.getItinerary().pickup();

        /*
         * A partner without configured service areas
         * is not eligible.
         */
        if (travelPartner.getServiceAreas().isEmpty()) {
            return false;
        }

        return travelPartner.getServiceAreas()
                .stream()
                .anyMatch(area ->
                        isWithinServiceArea(
                                pickup,
                                area
                        )
                );
    }

    private boolean isWithinServiceArea(
            Location location,
            ServiceArea serviceArea
    ) {

        if (location.latitude() == null
                || location.longitude() == null) {

            return false;
        }

        double distanceKm =
                calculateDistanceKm(
                        location.latitude(),
                        location.longitude(),
                        serviceArea.latitude(),
                        serviceArea.longitude()
                );

        return distanceKm <= serviceArea.radiusKm();
    }

    private double calculateDistanceKm(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {

        final double earthRadiusKm = 6371.0;

        double latitudeDifference =
                Math.toRadians(
                        latitude2 - latitude1
                );

        double longitudeDifference =
                Math.toRadians(
                        longitude2 - longitude1
                );

        double a =
                Math.sin(latitudeDifference / 2)
                        * Math.sin(latitudeDifference / 2)
                        + Math.cos(
                        Math.toRadians(latitude1)
                )
                        * Math.cos(
                        Math.toRadians(latitude2)
                )
                        * Math.sin(
                        longitudeDifference / 2
                )
                        * Math.sin(
                        longitudeDifference / 2
                );

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return earthRadiusKm * c;
    }
}