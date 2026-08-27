package com.yocabs.api.modules.pricing.infrastructure.persistence;

import com.yocabs.api.modules.pricing.domain.model.PricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.RentalPricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.OneWayPricingConfiguration;
import com.yocabs.api.modules.pricing.domain.model.RoundTripPricingConfiguration;
import com.yocabs.api.modules.triprequest.domain.model.TripType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "vehicle_pricing_configurations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_vehicle_pricing_trip_type",
                        columnNames = {
                                "vehicle_id",
                                "trip_type"
                        }
                )
        }
)
public class PricingConfigurationEntity {

    @Id
    private UUID id;

    @Column(
            name = "vehicle_id",
            nullable = false
    )
    private UUID vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "trip_type",
            nullable = false,
            length = 50
    )
    private TripType tripType;

    @Column(
            name = "active",
            nullable = false
    )
    private boolean active;

    @Column(name = "base_fee")
    private BigDecimal baseFee;

    @Column(name = "per_km_charge")
    private BigDecimal perKmCharge;

    @Column(name = "driver_allowance")
    private BigDecimal driverAllowance;

    @Column(name = "minimum_billable_km")
    private BigDecimal minimumBillableKm;

    @Column(name = "included_duration_minutes")
    private Integer includedDurationMinutes;

    @Column(name = "included_distance_km")
    private BigDecimal includedDistanceKm;

    @Column(name = "package_price")
    private BigDecimal packagePrice;

    @Column(name = "extra_hour_charge")
    private BigDecimal extraHourCharge;

    @Column(name = "extra_km_charge")
    private BigDecimal extraKmCharge;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    protected PricingConfigurationEntity() {
        // JPA
    }

    public PricingConfigurationEntity(
            UUID id,
            UUID vehicleId,
            TripType tripType,
            boolean active,
            BigDecimal baseFee,
            BigDecimal perKmCharge,
            BigDecimal driverAllowance,
            BigDecimal minimumBillableKm,
            Integer includedDurationMinutes,
            BigDecimal includedDistanceKm,
            BigDecimal packagePrice,
            BigDecimal extraHourCharge,
            BigDecimal extraKmCharge,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.tripType = tripType;
        this.active = active;
        this.baseFee = baseFee;
        this.perKmCharge = perKmCharge;
        this.driverAllowance = driverAllowance;
        this.minimumBillableKm = minimumBillableKm;
        this.includedDurationMinutes =
                includedDurationMinutes;
        this.includedDistanceKm = includedDistanceKm;
        this.packagePrice = packagePrice;
        this.extraHourCharge = extraHourCharge;
        this.extraKmCharge = extraKmCharge;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PricingConfigurationEntity
    fromDomain(
            PricingConfiguration configuration
    ) {

        Integer includedDurationMinutes = null;

        BigDecimal includedDistanceKm = null;
        BigDecimal packagePrice = null;
        BigDecimal extraHourCharge = null;
        BigDecimal extraKmCharge = null;

        BigDecimal baseFee = null;
        BigDecimal perKmCharge = null;
        BigDecimal driverAllowance = null;
        BigDecimal minimumBillableKm = null;

        if (configuration
                instanceof OneWayPricingConfiguration pricing) {

            baseFee = pricing.getBaseFee();
            perKmCharge = pricing.getPerKmCharge();
            driverAllowance =
                    pricing.getDriverAllowance();
            minimumBillableKm =
                    pricing.getMinimumBillableKm();

        } else if (configuration
                instanceof RoundTripPricingConfiguration pricing) {

            baseFee = pricing.getBaseFee();
            perKmCharge = pricing.getPerKmCharge();
            driverAllowance =
                    pricing.getDriverAllowance();
            minimumBillableKm =
                    pricing.getMinimumBillableKm();

        } else if (configuration
                instanceof RentalPricingConfiguration pricing) {

            includedDurationMinutes =
                    Math.toIntExact(
                            pricing
                                    .getIncludedDuration()
                                    .toMinutes()
                    );

            includedDistanceKm =
                    pricing.getIncludedDistanceKm();

            packagePrice =
                    pricing.getPackagePrice();

            extraHourCharge =
                    pricing.getExtraHourCharge();

            extraKmCharge =
                    pricing.getExtraKmCharge();

            driverAllowance =
                    pricing.getDriverAllowance();
        }

        return new PricingConfigurationEntity(
                configuration.getId(),
                configuration.getVehicleId(),
                configuration.getTripType(),
                configuration.isActive(),
                baseFee,
                perKmCharge,
                driverAllowance,
                minimumBillableKm,
                includedDurationMinutes,
                includedDistanceKm,
                packagePrice,
                extraHourCharge,
                extraKmCharge,
                configuration.getCreatedAt(),
                configuration.getUpdatedAt()
        );
    }

    public PricingConfiguration toDomain() {

        return switch (tripType) {

            case CHAUFFEUR_ONE_WAY ->
                    new OneWayPricingConfiguration(
                            id,
                            vehicleId,
                            baseFee,
                            perKmCharge,
                            driverAllowance,
                            minimumBillableKm,
                            active,
                            createdAt,
                            updatedAt
                    );

            case CHAUFFEUR_ROUND_TRIP ->
                    new RoundTripPricingConfiguration(
                            id,
                            vehicleId,
                            baseFee,
                            perKmCharge,
                            driverAllowance,
                            minimumBillableKm,
                            active,
                            createdAt,
                            updatedAt
                    );

            case CHAUFFEUR_RENTAL ->
                    new RentalPricingConfiguration(
                            id,
                            vehicleId,
                            Duration.ofMinutes(
                                    includedDurationMinutes
                            ),
                            includedDistanceKm,
                            packagePrice,
                            extraHourCharge,
                            extraKmCharge,
                            driverAllowance,
                            active,
                            createdAt,
                            updatedAt
                    );
        };
    }

    public UUID getId() {
        return id;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public TripType getTripType() {
        return tripType;
    }

    public boolean isActive() {
        return active;
    }

    public BigDecimal getBaseFee() {
        return baseFee;
    }

    public BigDecimal getPerKmCharge() {
        return perKmCharge;
    }

    public BigDecimal getDriverAllowance() {
        return driverAllowance;
    }

    public BigDecimal getMinimumBillableKm() {
        return minimumBillableKm;
    }

    public Integer getIncludedDurationMinutes() {
        return includedDurationMinutes;
    }

    public BigDecimal getIncludedDistanceKm() {
        return includedDistanceKm;
    }

    public BigDecimal getPackagePrice() {
        return packagePrice;
    }

    public BigDecimal getExtraHourCharge() {
        return extraHourCharge;
    }

    public BigDecimal getExtraKmCharge() {
        return extraKmCharge;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}