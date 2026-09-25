package com.yocabs.api.modules.vehicle.presentation.controller;

import com.yocabs.api.modules.document.application.DocumentService;
import com.yocabs.api.modules.document.application.DocumentService.Content;
import com.yocabs.api.modules.document.domain.Document;
import com.yocabs.api.modules.vehicle.application.service.FacilityService;
import com.yocabs.api.modules.vehicle.application.service.VehicleProfileService;
import com.yocabs.api.modules.vehicle.application.service.VehicleProfileService.PhotoView;
import com.yocabs.api.modules.vehicle.application.service.VehicleProfileService.ProfileView;
import com.yocabs.api.modules.vehicle.domain.model.Facility;
import com.yocabs.api.modules.vehicle.domain.model.FuelType;
import com.yocabs.api.modules.vehicle.domain.model.Transmission;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
public class VehicleProfileController {

    private final VehicleProfileService profileService;
    private final FacilityService facilityService;
    private final DocumentService documentService;

    public VehicleProfileController(
            VehicleProfileService profileService,
            FacilityService facilityService,
            DocumentService documentService
    ) {
        this.profileService = profileService;
        this.facilityService = facilityService;
        this.documentService = documentService;
    }

    @GetMapping("/api/v1/travel-partners/{travelPartnerId}/vehicles/{vehicleId}/profile")
    public ProfileResponse get(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId
    ) {
        return ProfileResponse.from(profileService.get(actor, travelPartnerId, vehicleId));
    }

    @PutMapping("/api/v1/travel-partners/{travelPartnerId}/vehicles/{vehicleId}/profile")
    public ProfileResponse update(
            @CurrentActor Actor actor,
            @PathVariable UUID travelPartnerId,
            @PathVariable UUID vehicleId,
            @RequestBody UpdateProfileRequest request
    ) {
        return ProfileResponse.from(
                profileService.update(
                        actor, travelPartnerId, vehicleId,
                        request.fuelType(), request.transmission(),
                        request.modelYear(), request.luggageCapacity(),
                        request.facilityCodes()
                )
        );
    }

    /** The catalogue partners choose from. */
    @GetMapping("/api/v1/facilities")
    public List<FacilityResponse> facilities() {
        return facilityService.listActive().stream().map(FacilityResponse::from).toList();
    }

    @GetMapping("/api/v1/admin/facilities")
    public List<FacilityResponse> allFacilities(@CurrentActor Actor actor) {
        return facilityService.listAll(actor).stream().map(FacilityResponse::from).toList();
    }

    @PostMapping("/api/v1/admin/facilities")
    @ResponseStatus(HttpStatus.CREATED)
    public FacilityResponse createFacility(
            @CurrentActor Actor actor,
            @RequestBody CreateFacilityRequest request
    ) {
        return FacilityResponse.from(facilityService.create(actor, request.code(), request.name()));
    }

    @PostMapping("/api/v1/admin/facilities/{code}/activate")
    public FacilityResponse activateFacility(@CurrentActor Actor actor, @PathVariable String code) {
        return FacilityResponse.from(facilityService.setActive(actor, code, true));
    }

    @PostMapping("/api/v1/admin/facilities/{code}/deactivate")
    public FacilityResponse deactivateFacility(@CurrentActor Actor actor, @PathVariable String code) {
        return FacilityResponse.from(facilityService.setActive(actor, code, false));
    }

    /** Public: only admin-approved vehicle photos are ever served here. */
    @GetMapping("/api/v1/vehicles/{vehicleId}/photos/{documentId}")
    public ResponseEntity<byte[]> photo(
            @PathVariable UUID vehicleId,
            @PathVariable UUID documentId
    ) {
        Content content = documentService.downloadApprovedVehiclePhoto(vehicleId, documentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(content.bytes());
    }

    public record CreateFacilityRequest(String code, String name) {
    }

    public record UpdateProfileRequest(
            FuelType fuelType,
            Transmission transmission,
            Integer modelYear,
            Integer luggageCapacity,
            Set<String> facilityCodes
    ) {
    }

    public record FacilityResponse(String code, String name, boolean active) {

        static FacilityResponse from(Facility facility) {
            return new FacilityResponse(facility.code(), facility.name(), facility.active());
        }
    }

    public record PhotoResponse(UUID documentId, Document.Status status, String rejectionReason) {

        static PhotoResponse from(PhotoView photo) {
            return new PhotoResponse(photo.documentId(), photo.status(), photo.rejectionReason());
        }
    }

    public record ProfileResponse(
            UUID vehicleId,
            FuelType fuelType,
            Transmission transmission,
            Integer modelYear,
            Integer luggageCapacity,
            List<FacilityResponse> facilities,
            List<PhotoResponse> photos
    ) {

        static ProfileResponse from(ProfileView view) {
            return new ProfileResponse(
                    view.profile().getVehicleId(),
                    view.profile().getFuelType(),
                    view.profile().getTransmission(),
                    view.profile().getModelYear(),
                    view.profile().getLuggageCapacity(),
                    view.facilities().stream().map(FacilityResponse::from).toList(),
                    view.photos().stream().map(PhotoResponse::from).toList()
            );
        }
    }
}
