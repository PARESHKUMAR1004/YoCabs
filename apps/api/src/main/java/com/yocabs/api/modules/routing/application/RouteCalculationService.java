package com.yocabs.api.modules.routing.application;

import com.yocabs.api.modules.routing.domain.RouteCalculation;
import com.yocabs.api.modules.triprequest.domain.valueobject.Itinerary;

public interface RouteCalculationService {

    RouteCalculation calculate(
            Itinerary itinerary
    );
}