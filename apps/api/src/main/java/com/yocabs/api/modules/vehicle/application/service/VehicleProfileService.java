package com.yocabs.api.modules.vehicle.application.service;

import com.yocabs.api.modules.document.domain.Document;
import com.yocabs.api.modules.document.domain.DocumentRepository;
import com.yocabs.api.modules.vehicle.domain.model.Facility;
import com.yocabs.api.modules.vehicle.domain.model.FuelType;
import com.yocabs.api.modules.vehicle.domain.model.Transmission;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.model.VehicleProfile;
import com.yocabs.api.modules.vehicle.domain.repository.FacilityRepository;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleProfileRepository;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class VehicleProfileService {

    private final VehicleRepository vehicles;
    private final VehicleProfileRepository profiles;
    private final FacilityRepository facilities;
    private final DocumentRepository documents;

    public VehicleProfileService(
            VehicleRepository vehicles,
            VehicleProfileRepository profiles,
            FacilityRepository facilities,
            DocumentRepository documents
    ) {
        this.vehicles = vehicles;
        this.profiles = profiles;
        this.facilities = facilities;
        this.documents = documents;
    }

    @Transactional(readOnly = true)
    public ProfileView get(Actor actor, UUID travelPartnerId, UUID vehicleId) {

        requireVehicle(actor, travelPartnerId, vehicleId);

        return view(profiles.find(vehicleId).orElseGet(() -> VehicleProfile.empty(vehicleId)));
    }

    /**
     * Details are replaced as a whole; facilities are replaced only when a list
     * is supplied (null leaves them untouched).
     */
    @Transactional
    public ProfileView update(
            Actor actor,
            UUID travelPartnerId,
            UUID vehicleId,
            FuelType fuelType,
            Transmission transmission,
            Integer modelYear,
            Integer luggageCapacity,
            Set<String> facilityCodes
    ) {
        requireVehicle(actor, travelPartnerId, vehicleId);

        VehicleProfile profile = profiles.find(vehicleId).orElseGet(() -> VehicleProfile.empty(vehicleId));

        profile.updateDetails(fuelType, transmission, modelYear, luggageCapacity);

        if (facilityCodes != null) {
            profile.replaceFacilities(validatedCodes(facilityCodes));
        }

        return view(profiles.save(profile));
    }

    private Set<String> validatedCodes(Set<String> requested) {

        Set<String> codes = new TreeSet<>();
        for (String code : requested) {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("Facility codes cannot be blank");
            }
            codes.add(code.trim().toUpperCase());
        }

        Set<String> active = new TreeSet<>();
        for (Facility facility : facilities.findByCodes(codes)) {
            if (facility.active()) {
                active.add(facility.code());
            }
        }

        Set<String> unknown = new TreeSet<>(codes);
        unknown.removeAll(active);

        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown or inactive facilities: " + unknown);
        }

        return codes;
    }

    private ProfileView view(VehicleProfile profile) {

        List<Facility> chosen =
                profile.getFacilityCodes().isEmpty()
                        ? List.of()
                        : facilities.findByCodes(profile.getFacilityCodes());

        List<PhotoView> photos =
                documents.findByOwner(Document.OwnerType.VEHICLE, profile.getVehicleId()).stream()
                        .filter(document -> document.getType() == Document.Type.VEHICLE_PHOTO)
                        .map(document -> new PhotoView(
                                document.getId(), document.getStatus(), document.getRejectionReason()))
                        .toList();

        return new ProfileView(profile, chosen, photos);
    }

    private void requireVehicle(Actor actor, UUID travelPartnerId, UUID vehicleId) {

        actor.requirePartnerAccess(travelPartnerId);

        Vehicle vehicle =
                vehicles.findById(vehicleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));

        if (!vehicle.getTravelPartnerId().equals(travelPartnerId)) {
            throw new ResourceNotFoundException("Vehicle not found: " + vehicleId);
        }
    }

    public record ProfileView(VehicleProfile profile, List<Facility> facilities, List<PhotoView> photos) {
    }

    public record PhotoView(UUID documentId, Document.Status status, String rejectionReason) {
    }
}
