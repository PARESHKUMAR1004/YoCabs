package com.yocabs.api.modules.explore.interfaces.rest;

import com.yocabs.api.modules.explore.application.ExploreService;
import com.yocabs.api.modules.explore.application.ExploreService.PartnerDetail;
import com.yocabs.api.modules.explore.application.ExploreService.PartnerSummary;
import com.yocabs.api.modules.explore.application.ExploreService.VehicleView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Public, like a trip search: a traveller can look around before signing in. */
@RestController
@RequestMapping("/api/v1/explore")
public class ExploreController {

    private final ExploreService exploreService;

    public ExploreController(ExploreService exploreService) {
        this.exploreService = exploreService;
    }

    @GetMapping("/partners")
    public List<PartnerResponse> partners(
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return exploreService.partnersNear(latitude, longitude).stream()
                .map(PartnerResponse::from)
                .toList();
    }

    @GetMapping("/partners/{partnerId}")
    public PartnerDetailResponse partner(
            @PathVariable UUID partnerId,
            @RequestParam double latitude,
            @RequestParam double longitude
    ) {
        return PartnerDetailResponse.from(exploreService.partner(partnerId, latitude, longitude));
    }

    private static String photoPath(UUID vehicleId, UUID photoId) {
        return "/api/v1/vehicles/" + vehicleId + "/photos/" + photoId;
    }

    public record PartnerResponse(
            UUID id,
            String name,
            double rating,
            long reviewCount,
            int vehicleCount,
            List<String> categories,
            String coverPhoto
    ) {
        static PartnerResponse from(PartnerSummary summary) {
            return new PartnerResponse(
                    summary.id(),
                    summary.name(),
                    summary.rating(),
                    summary.reviewCount(),
                    summary.vehicleCount(),
                    summary.categories(),
                    summary.coverVehicleId() == null
                            ? null
                            : photoPath(summary.coverVehicleId(), summary.coverPhotoId())
            );
        }
    }

    public record FacilityResponse(String code, String name) {
    }

    public record VehicleResponse(
            UUID id,
            String make,
            String model,
            String category,
            int passengerCapacity,
            String fuelType,
            String transmission,
            Integer modelYear,
            Integer luggageCapacity,
            List<FacilityResponse> facilities,
            List<String> photos
    ) {
        static VehicleResponse from(VehicleView view) {
            var vehicle = view.vehicle();
            var showcase = view.showcase();

            return new VehicleResponse(
                    vehicle.getId(),
                    vehicle.getMake(),
                    vehicle.getModel(),
                    vehicle.getCategory().name(),
                    vehicle.getPassengerCapacity(),
                    showcase.fuelType() == null ? null : showcase.fuelType().name(),
                    showcase.transmission() == null ? null : showcase.transmission().name(),
                    showcase.modelYear(),
                    showcase.luggageCapacity(),
                    showcase.facilities().stream()
                            .map(facility -> new FacilityResponse(facility.code(), facility.name()))
                            .toList(),
                    showcase.photoIds().stream().map(id -> photoPath(vehicle.getId(), id)).toList()
            );
        }
    }

    public record PartnerDetailResponse(PartnerResponse partner, List<VehicleResponse> vehicles) {
        static PartnerDetailResponse from(PartnerDetail detail) {
            return new PartnerDetailResponse(
                    PartnerResponse.from(detail.partner()),
                    detail.vehicles().stream().map(VehicleResponse::from).toList()
            );
        }
    }
}
