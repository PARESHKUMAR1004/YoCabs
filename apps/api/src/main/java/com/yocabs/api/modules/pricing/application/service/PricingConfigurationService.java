package com.yocabs.api.modules.pricing.application.service;

import com.yocabs.api.modules.pricing.domain.model.OneWayPricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.RentalPricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.RoundTripPricingConfiguration;
import com.yocabs.api.modules.pricing.domain.repository.PricingConfigurationRepository;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class PricingConfigurationService {

    private final PricingConfigurationRepository
            pricingConfigurationRepository;

    public PricingConfigurationService(
            PricingConfigurationRepository
                    pricingConfigurationRepository
    ) {
        this.pricingConfigurationRepository =
                pricingConfigurationRepository;
    }

    @Transactional
    public PricingConfiguration create(
            CreatePricingConfigurationCommand command
    ) {

        validateCommand(command);

        pricingConfigurationRepository
                .findByVehicleIdAndTripType(
                        command.vehicleId(),
                        command.tripType()
                )
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "Pricing configuration already exists "
                                    + "for vehicle "
                                    + command.vehicleId()
                                    + " and trip type "
                                    + command.tripType()
                    );
                });

        PricingConfiguration configuration =
                buildConfiguration(command);

        return pricingConfigurationRepository
                .create(configuration);
    }

    @Transactional
    public PricingConfiguration update(
            UUID pricingConfigurationId,
            UpdatePricingConfigurationCommand command
    ) {

        if (pricingConfigurationId == null) {
            throw new IllegalArgumentException(
                    "Pricing configuration ID is required"
            );
        }

        validateCommand(command);

        PricingConfiguration existing =
                pricingConfigurationRepository
                        .findById(pricingConfigurationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Pricing configuration not found: "
                                                + pricingConfigurationId
                                )
                        );

        if (!existing.getVehicleId()
                .equals(command.vehicleId())) {

            throw new IllegalArgumentException(
                    "Pricing configuration does not belong "
                            + "to the specified vehicle"
            );
        }

        if (existing.getTripType()
                != command.tripType()) {

            throw new IllegalArgumentException(
                    "Pricing configuration trip type "
                            + "cannot be changed"
            );
        }

        updateConfiguration(
                existing,
                command
        );

        return pricingConfigurationRepository
                .update(existing);
    }

    @Transactional(readOnly = true)
    public PricingConfiguration getById(
            UUID pricingConfigurationId
    ) {

        if (pricingConfigurationId == null) {
            throw new IllegalArgumentException(
                    "Pricing configuration ID is required"
            );
        }

        return pricingConfigurationRepository
                .findById(pricingConfigurationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Pricing configuration not found: "
                                        + pricingConfigurationId
                        )
                );
    }

    @Transactional(readOnly = true)
    public PricingConfiguration getByVehicleAndTripType(
            UUID vehicleId,
            TripType tripType
    ) {

        if (vehicleId == null) {
            throw new IllegalArgumentException(
                    "Vehicle ID is required"
            );
        }

        if (tripType == null) {
            throw new IllegalArgumentException(
                    "Trip type is required"
            );
        }

        return pricingConfigurationRepository
                .findByVehicleIdAndTripType(
                        vehicleId,
                        tripType
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Pricing configuration not found "
                                        + "for vehicle "
                                        + vehicleId
                                        + " and trip type "
                                        + tripType
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<PricingConfiguration>
    getByVehicle(
            UUID vehicleId
    ) {

        if (vehicleId == null) {
            throw new IllegalArgumentException(
                    "Vehicle ID is required"
            );
        }

        return pricingConfigurationRepository
                .findByVehicleId(vehicleId);
    }

    @Transactional
    public PricingConfiguration activate(
            UUID pricingConfigurationId
    ) {

        PricingConfiguration configuration =
                getById(pricingConfigurationId);

        configuration.activate();

        return pricingConfigurationRepository
                .update(configuration);
    }

    @Transactional
    public PricingConfiguration deactivate(
            UUID pricingConfigurationId
    ) {

        PricingConfiguration configuration =
                getById(pricingConfigurationId);

        configuration.deactivate();

        return pricingConfigurationRepository
                .update(configuration);
    }

    private PricingConfiguration buildConfiguration(
            CreatePricingConfigurationCommand command
    ) {

        UUID id = UUID.randomUUID();

        return switch (command.tripType()) {

            case CHAUFFEUR_ONE_WAY ->
                    new OneWayPricingConfiguration(
                            id,
                            command.vehicleId(),
                            required(
                                    command.baseFee(),
                                    "Base fee"
                            ),
                            required(
                                    command.perKmCharge(),
                                    "Per km charge"
                            ),
                            required(
                                    command.driverAllowance(),
                                    "Driver allowance"
                            ),
                            required(
                                    command.minimumBillableKm(),
                                    "Minimum billable km"
                            )
                    );

            case CHAUFFEUR_ROUND_TRIP ->
                    new RoundTripPricingConfiguration(
                            id,
                            command.vehicleId(),
                            required(
                                    command.baseFee(),
                                    "Base fee"
                            ),
                            required(
                                    command.perKmCharge(),
                                    "Per km charge"
                            ),
                            required(
                                    command.driverAllowance(),
                                    "Driver allowance"
                            ),
                            required(
                                    command.minimumBillableKm(),
                                    "Minimum billable km"
                            )
                    );

            case CHAUFFEUR_RENTAL ->
                    new RentalPricingConfiguration(
                            id,
                            command.vehicleId(),
                            Duration.ofMinutes(
                                    required(
                                            command.includedDurationMinutes(),
                                            "Included duration"
                                    )
                            ),
                            required(
                                    command.includedDistanceKm(),
                                    "Included distance"
                            ),
                            required(
                                    command.packagePrice(),
                                    "Package price"
                            ),
                            required(
                                    command.extraHourCharge(),
                                    "Extra hour charge"
                            ),
                            required(
                                    command.extraKmCharge(),
                                    "Extra km charge"
                            ),
                            required(
                                    command.driverAllowance(),
                                    "Driver allowance"
                            )
                    );
        };
    }

    private void updateConfiguration(
            PricingConfiguration configuration,
            UpdatePricingConfigurationCommand command
    ) {

        switch (configuration.getTripType()) {

            case CHAUFFEUR_ONE_WAY -> {

                OneWayPricingConfiguration pricing =
                        (OneWayPricingConfiguration)
                                configuration;

                pricing.update(
                        required(
                                command.baseFee(),
                                "Base fee"
                        ),
                        required(
                                command.perKmCharge(),
                                "Per km charge"
                        ),
                        required(
                                command.driverAllowance(),
                                "Driver allowance"
                        ),
                        required(
                                command.minimumBillableKm(),
                                "Minimum billable km"
                        )
                );
            }

            case CHAUFFEUR_ROUND_TRIP -> {

                RoundTripPricingConfiguration pricing =
                        (RoundTripPricingConfiguration)
                                configuration;

                pricing.update(
                        required(
                                command.baseFee(),
                                "Base fee"
                        ),
                        required(
                                command.perKmCharge(),
                                "Per km charge"
                        ),
                        required(
                                command.driverAllowance(),
                                "Driver allowance"
                        ),
                        required(
                                command.minimumBillableKm(),
                                "Minimum billable km"
                        )
                );
            }

            case CHAUFFEUR_RENTAL -> {

                RentalPricingConfiguration pricing =
                        (RentalPricingConfiguration)
                                configuration;

                pricing.update(
                        Duration.ofMinutes(
                                required(
                                        command.includedDurationMinutes(),
                                        "Included duration"
                                )
                        ),
                        required(
                                command.includedDistanceKm(),
                                "Included distance"
                        ),
                        required(
                                command.packagePrice(),
                                "Package price"
                        ),
                        required(
                                command.extraHourCharge(),
                                "Extra hour charge"
                        ),
                        required(
                                command.extraKmCharge(),
                                "Extra km charge"
                        ),
                        required(
                                command.driverAllowance(),
                                "Driver allowance"
                        )
                );
            }
        }
    }

    private void validateCommand(
            CreatePricingConfigurationCommand command
    ) {

        if (command == null) {
            throw new IllegalArgumentException(
                    "Pricing configuration command is required"
            );
        }

        if (command.vehicleId() == null) {
            throw new IllegalArgumentException(
                    "Vehicle ID is required"
            );
        }

        if (command.tripType() == null) {
            throw new IllegalArgumentException(
                    "Trip type is required"
            );
        }
    }

    private void validateCommand(
            UpdatePricingConfigurationCommand command
    ) {

        if (command == null) {
            throw new IllegalArgumentException(
                    "Pricing configuration command is required"
            );
        }

        if (command.vehicleId() == null) {
            throw new IllegalArgumentException(
                    "Vehicle ID is required"
            );
        }

        if (command.tripType() == null) {
            throw new IllegalArgumentException(
                    "Trip type is required"
            );
        }
    }

    private <T> T required(
            T value,
            String field
    ) {

        if (value == null) {
            throw new IllegalArgumentException(
                    field + " is required"
            );
        }

        return value;
    }

    public record CreatePricingConfigurationCommand(
            UUID vehicleId,
            TripType tripType,
            BigDecimal baseFee,
            BigDecimal perKmCharge,
            BigDecimal driverAllowance,
            BigDecimal minimumBillableKm,
            Integer includedDurationMinutes,
            BigDecimal includedDistanceKm,
            BigDecimal packagePrice,
            BigDecimal extraHourCharge,
            BigDecimal extraKmCharge
    ) {
    }

    public record UpdatePricingConfigurationCommand(
            UUID vehicleId,
            TripType tripType,
            BigDecimal baseFee,
            BigDecimal perKmCharge,
            BigDecimal driverAllowance,
            BigDecimal minimumBillableKm,
            Integer includedDurationMinutes,
            BigDecimal includedDistanceKm,
            BigDecimal packagePrice,
            BigDecimal extraHourCharge,
            BigDecimal extraKmCharge
    ) {
    }
}