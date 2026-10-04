package com.yocabs.api.modules.popularplace.interfaces.rest;

import com.yocabs.api.modules.popularplace.application.PopularPlaceService;
import com.yocabs.api.modules.popularplace.domain.PopularPlace;
import com.yocabs.api.shared.security.Actor;
import com.yocabs.api.shared.security.CurrentActor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class PopularPlaceController {

    private final PopularPlaceService popularPlaceService;

    public PopularPlaceController(PopularPlaceService popularPlaceService) {
        this.popularPlaceService = popularPlaceService;
    }

    /** Public, like a trip search: the app shows these before anyone signs in. */
    @GetMapping("/api/v1/popular-places")
    public List<PopularPlaceResponse> list() {
        return popularPlaceService.list().stream().map(PopularPlaceResponse::from).toList();
    }

    @PostMapping("/api/v1/admin/popular-places")
    public PopularPlaceResponse create(
            @CurrentActor Actor actor,
            @RequestBody PopularPlaceRequest request
    ) {
        return PopularPlaceResponse.from(
                popularPlaceService.create(
                        actor, request.requiredName(), request.subtitle(),
                        request.requiredLatitude(), request.requiredLongitude(), request.orderOrDefault()
                )
        );
    }

    @PutMapping("/api/v1/admin/popular-places/{id}")
    public PopularPlaceResponse update(
            @CurrentActor Actor actor,
            @PathVariable UUID id,
            @RequestBody PopularPlaceRequest request
    ) {
        return PopularPlaceResponse.from(
                popularPlaceService.update(
                        actor, id, request.requiredName(), request.subtitle(),
                        request.requiredLatitude(), request.requiredLongitude(), request.orderOrDefault()
                )
        );
    }

    @DeleteMapping("/api/v1/admin/popular-places/{id}")
    public void delete(@CurrentActor Actor actor, @PathVariable UUID id) {
        popularPlaceService.delete(actor, id);
    }

    public record PopularPlaceRequest(
            String name,
            String subtitle,
            Double latitude,
            Double longitude,
            Integer displayOrder
    ) {
        String requiredName() {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Name is required");
            }
            return name;
        }

        double requiredLatitude() {
            if (latitude == null) {
                throw new IllegalArgumentException("Latitude is required");
            }
            return latitude;
        }

        double requiredLongitude() {
            if (longitude == null) {
                throw new IllegalArgumentException("Longitude is required");
            }
            return longitude;
        }

        int orderOrDefault() {
            return displayOrder == null ? 0 : displayOrder;
        }
    }

    public record PopularPlaceResponse(
            UUID id,
            String name,
            String subtitle,
            double latitude,
            double longitude,
            int displayOrder,
            Instant updatedAt
    ) {
        static PopularPlaceResponse from(PopularPlace place) {
            return new PopularPlaceResponse(
                    place.id(), place.name(), place.subtitle(),
                    place.latitude(), place.longitude(), place.displayOrder(), place.updatedAt()
            );
        }
    }
}
