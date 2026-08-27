package com.yocabs.api.modules.travelpartner.domain.model;

import com.yocabs.api.modules.travelpartner.domain.valueobject.ServiceArea;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TravelPartner {

    private final UUID id;

    private String name;

    private TravelPartnerStatus status;

    private final List<ServiceArea> serviceAreas;

    private final Instant createdAt;

    private Instant updatedAt;

    private TravelPartner(
            UUID id,
            String name,
            TravelPartnerStatus status,
            List<ServiceArea> serviceAreas,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.serviceAreas = new ArrayList<>(serviceAreas);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TravelPartner create(
            String name
    ) {
        validateName(name);

        Instant now = Instant.now();

        return new TravelPartner(
                UUID.randomUUID(),
                name.trim(),
                TravelPartnerStatus.PENDING_APPROVAL,
                List.of(),
                now,
                now
        );
    }

    public void activate() {

        if (status != TravelPartnerStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                    "Only a partner pending approval can be activated"
            );
        }

        status = TravelPartnerStatus.ACTIVE;
        updatedAt = Instant.now();
    }

    public void suspend() {

        if (status != TravelPartnerStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only an active partner can be suspended"
            );
        }

        status = TravelPartnerStatus.SUSPENDED;
        updatedAt = Instant.now();
    }

    public void deactivate() {

        if (status == TravelPartnerStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Travel partner is already inactive"
            );
        }

        status = TravelPartnerStatus.INACTIVE;
        updatedAt = Instant.now();
    }

    public void addServiceArea(
            ServiceArea serviceArea
    ) {
        if (serviceArea == null) {
            throw new IllegalArgumentException(
                    "Service area is required"
            );
        }

        boolean alreadyExists =
                serviceAreas.stream()
                        .anyMatch(existing ->
                                existing.id().equals(
                                        serviceArea.id()
                                )
                        );

        if (alreadyExists) {
            throw new IllegalArgumentException(
                    "Service area already exists: "
                            + serviceArea.id()
            );
        }

        serviceAreas.add(serviceArea);
        updatedAt = Instant.now();
    }

    public void updateServiceArea(
            UUID serviceAreaId,
            String name,
            double latitude,
            double longitude,
            double radiusKm
    ) {

        int index = findServiceAreaIndex(
                serviceAreaId
        );

        ServiceArea updated =
                new ServiceArea(
                        serviceAreaId,
                        name,
                        latitude,
                        longitude,
                        radiusKm
                );

        serviceAreas.set(index, updated);

        updatedAt = Instant.now();
    }

    public void removeServiceArea(
            UUID serviceAreaId
    ) {

        int index = findServiceAreaIndex(
                serviceAreaId
        );

        serviceAreas.remove(index);

        updatedAt = Instant.now();
    }

    private int findServiceAreaIndex(
            UUID serviceAreaId
    ) {

        if (serviceAreaId == null) {
            throw new IllegalArgumentException(
                    "Service area ID is required"
            );
        }

        for (int i = 0; i < serviceAreas.size(); i++) {

            if (serviceAreas.get(i)
                    .id()
                    .equals(serviceAreaId)) {

                return i;
            }
        }

        throw new IllegalArgumentException(
                "Service area not found: "
                        + serviceAreaId
        );
    }

    private static void validateName(
            String name
    ) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Travel partner name is required"
            );
        }

        if (name.length() > 200) {
            throw new IllegalArgumentException(
                    "Travel partner name cannot exceed 200 characters"
            );
        }
    }

    public static TravelPartner reconstitute(
            UUID id,
            String name,
            TravelPartnerStatus status,
            List<ServiceArea> serviceAreas,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Travel partner ID is required"
            );
        }

        validateName(name);

        if (status == null) {
            throw new IllegalArgumentException(
                    "Travel partner status is required"
            );
        }

        if (serviceAreas == null) {
            throw new IllegalArgumentException(
                    "Service areas are required"
            );
        }

        if (createdAt == null) {
            throw new IllegalArgumentException(
                    "Created timestamp is required"
            );
        }

        if (updatedAt == null) {
            throw new IllegalArgumentException(
                    "Updated timestamp is required"
            );
        }

        return new TravelPartner(
                id,
                name,
                status,
                serviceAreas,
                createdAt,
                updatedAt
        );
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TravelPartnerStatus getStatus() {
        return status;
    }

    public List<ServiceArea> getServiceAreas() {
        return List.copyOf(serviceAreas);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }


}