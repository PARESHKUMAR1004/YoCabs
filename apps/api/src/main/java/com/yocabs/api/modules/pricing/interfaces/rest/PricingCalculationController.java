package com.yocabs.api.modules.pricing.interfaces.rest;

import com.yocabs.api.modules.pricing.application.service.CalculatePriceService;
import com.yocabs.api.modules.pricing.domain.PriceCalculation;
import com.yocabs.api.modules.pricing.domain.PricingContext;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pricing")
public class PricingCalculationController {

    private final CalculatePriceService calculatePriceService;

    public PricingCalculationController(
            CalculatePriceService calculatePriceService
    ) {
        this.calculatePriceService =
                calculatePriceService;
    }

    @PostMapping("/calculate")
    public PriceCalculationResponse calculate(
            @RequestBody CalculatePriceRequest request
    ) {

        PricingContext context =
                new PricingContext(
                        request.distanceKm(),
                        request.durationMinutes() == null
                                ? null
                                : Duration.ofMinutes(
                                request.durationMinutes()
                        ),
                        request.startTime(),
                        request.endTime()
                );

        PriceCalculation calculation =
                calculatePriceService.calculate(
                        request.vehicleId(),
                        request.tripType(),
                        context
                );

        return PriceCalculationResponse.from(
                calculation
        );
    }

    public record CalculatePriceRequest(
            UUID vehicleId,
            TripType tripType,
            BigDecimal distanceKm,
            Long durationMinutes,
            Instant startTime,
            Instant endTime
    ) {
    }

    public record PriceCalculationResponse(
            BigDecimal totalAmount,
            String currency,
            List<PriceComponentResponse> components
    ) {

        public static PriceCalculationResponse from(
                PriceCalculation calculation
        ) {

            return new PriceCalculationResponse(
                    calculation.totalAmount(),
                    calculation.currency(),
                    calculation.components()
                            .stream()
                            .map(component ->
                                    new PriceComponentResponse(
                                            component.code(),
                                            component.description(),
                                            component.amount()
                                    )
                            )
                            .toList()
            );
        }
    }

    public record PriceComponentResponse(
            String code,
            String description,
            BigDecimal amount
    ) {
    }
}