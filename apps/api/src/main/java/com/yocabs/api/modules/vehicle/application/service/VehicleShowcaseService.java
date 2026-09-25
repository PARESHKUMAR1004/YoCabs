package com.yocabs.api.modules.vehicle.application.service;

import com.yocabs.api.modules.document.domain.Document;
import com.yocabs.api.modules.document.domain.DocumentRepository;
import com.yocabs.api.modules.vehicle.domain.model.Facility;
import com.yocabs.api.modules.vehicle.domain.model.FuelType;
import com.yocabs.api.modules.vehicle.domain.model.Transmission;
import com.yocabs.api.modules.vehicle.domain.model.VehicleProfile;
import com.yocabs.api.modules.vehicle.domain.repository.FacilityRepository;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tourist-facing vehicle presentation data (details, active facilities, approved photos). */
@Service
public class VehicleShowcaseService {

    private final VehicleProfileRepository profiles;
    private final FacilityRepository facilities;
    private final DocumentRepository documents;

    public VehicleShowcaseService(
            VehicleProfileRepository profiles,
            FacilityRepository facilities,
            DocumentRepository documents
    ) {
        this.profiles = profiles;
        this.facilities = facilities;
        this.documents = documents;
    }

    /** Batched: three queries regardless of how many vehicles are shown. */
    @Transactional(readOnly = true)
    public Map<UUID, Showcase> forVehicles(Collection<UUID> vehicleIds) {

        Map<UUID, VehicleProfile> byVehicle = profiles.findAll(vehicleIds);

        Map<String, Facility> catalogue = new HashMap<>();
        facilities.findAll(true).forEach(facility -> catalogue.put(facility.code(), facility));

        Map<UUID, List<UUID>> photos = new HashMap<>();
        documents.findByOwnersAndTypeAndStatus(
                Document.OwnerType.VEHICLE, vehicleIds, Document.Type.VEHICLE_PHOTO, Document.Status.APPROVED
        ).forEach(document ->
                photos.computeIfAbsent(document.getOwnerId(), key -> new ArrayList<>()).add(document.getId()));

        Map<UUID, Showcase> result = new HashMap<>();

        for (UUID vehicleId : vehicleIds) {
            VehicleProfile profile = byVehicle.get(vehicleId);

            List<Facility> shown =
                    profile == null
                            ? List.of()
                            : profile.getFacilityCodes().stream()
                            .map(catalogue::get)
                            .filter(java.util.Objects::nonNull)
                            .sorted(java.util.Comparator.comparing(Facility::name))
                            .toList();

            result.put(
                    vehicleId,
                    new Showcase(
                            profile == null ? null : profile.getFuelType(),
                            profile == null ? null : profile.getTransmission(),
                            profile == null ? null : profile.getModelYear(),
                            profile == null ? null : profile.getLuggageCapacity(),
                            shown,
                            photos.getOrDefault(vehicleId, List.of())
                    )
            );
        }

        return result;
    }

    public record Showcase(
            FuelType fuelType,
            Transmission transmission,
            Integer modelYear,
            Integer luggageCapacity,
            List<Facility> facilities,
            List<UUID> photoIds
    ) {
    }
}
