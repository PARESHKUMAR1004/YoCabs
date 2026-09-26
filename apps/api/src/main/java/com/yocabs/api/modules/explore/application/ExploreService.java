package com.yocabs.api.modules.explore.application;

import com.yocabs.api.modules.review.application.ReviewService;
import com.yocabs.api.modules.review.domain.ReviewRepository.RatingSummary;
import com.yocabs.api.modules.travelpartner.domain.model.TravelPartner;
import com.yocabs.api.modules.travelpartner.domain.repository.TravelPartnerRepository;
import com.yocabs.api.modules.vehicle.application.service.VehicleShowcaseService;
import com.yocabs.api.modules.vehicle.application.service.VehicleShowcaseService.Showcase;
import com.yocabs.api.modules.vehicle.domain.model.Vehicle;
import com.yocabs.api.modules.vehicle.domain.repository.VehicleRepository;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Browsing the marketplace by who runs the cars, rather than by a trip: which verified travel
 * partners have a vehicle working around a point, and what those vehicles are. A vehicle counts
 * when it is available and its own service area covers the point, the same rule a search applies.
 */
@Service
public class ExploreService {

    private final TravelPartnerRepository partners;
    private final VehicleRepository vehicles;
    private final VehicleShowcaseService showcase;
    private final ReviewService reviews;

    public ExploreService(
            TravelPartnerRepository partners,
            VehicleRepository vehicles,
            VehicleShowcaseService showcase,
            ReviewService reviews
    ) {
        this.partners = partners;
        this.vehicles = vehicles;
        this.showcase = showcase;
        this.reviews = reviews;
    }

    /** Every active partner with at least one vehicle working around the point, best rated first. */
    @Transactional(readOnly = true)
    public List<PartnerSummary> partnersNear(double latitude, double longitude) {

        Coverage coverage = coverage(latitude, longitude);

        if (coverage.vehicles().isEmpty()) {
            return List.of();
        }

        Map<UUID, Showcase> shown = showcaseFor(coverage);

        return coverage.vehicles().entrySet().stream()
                .map(entry -> summarize(coverage.partners().get(entry.getKey()), entry.getValue(), shown))
                .sorted(Comparator
                        .comparingDouble(PartnerSummary::rating).reversed()
                        .thenComparing(PartnerSummary::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** One partner and the vehicles of theirs that work around the point. */
    @Transactional(readOnly = true)
    public PartnerDetail partner(UUID partnerId, double latitude, double longitude) {

        Coverage coverage = coverage(latitude, longitude);
        List<Vehicle> theirs = coverage.vehicles().get(partnerId);

        if (theirs == null) {
            throw new ResourceNotFoundException("This travel partner does not operate in your area");
        }

        Map<UUID, Showcase> shown = showcaseFor(coverage);

        List<VehicleView> views = theirs.stream()
                .sorted(Comparator.comparing(Vehicle::getMake).thenComparing(Vehicle::getModel))
                .map(vehicle -> new VehicleView(vehicle, shown.get(vehicle.getId())))
                .toList();

        return new PartnerDetail(
                summarize(coverage.partners().get(partnerId), theirs, shown),
                views
        );
    }

    private PartnerSummary summarize(TravelPartner partner, List<Vehicle> theirs, Map<UUID, Showcase> shown) {

        RatingSummary rating = reviews.summary(partner.getId());

        // The first vehicle that has a photo lends it to the partner's card.
        UUID coverVehicle = null;
        UUID coverPhoto = null;

        for (Vehicle vehicle : theirs) {
            Showcase details = shown.get(vehicle.getId());
            if (details != null && !details.photoIds().isEmpty()) {
                coverVehicle = vehicle.getId();
                coverPhoto = details.photoIds().getFirst();
                break;
            }
        }

        return new PartnerSummary(
                partner.getId(),
                partner.getName(),
                rating.average(),
                rating.count(),
                theirs.size(),
                theirs.stream().map(vehicle -> vehicle.getCategory().name()).distinct().sorted().toList(),
                coverVehicle,
                coverPhoto
        );
    }

    private Map<UUID, Showcase> showcaseFor(Coverage coverage) {
        List<UUID> ids = new ArrayList<>();
        coverage.vehicles().values().forEach(list -> list.forEach(vehicle -> ids.add(vehicle.getId())));
        return showcase.forVehicles(ids);
    }

    private Coverage coverage(double latitude, double longitude) {

        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("That is not a valid location");
        }

        Map<UUID, TravelPartner> active = partners.findActivePartners().stream()
                .collect(Collectors.toMap(TravelPartner::getId, Function.identity()));

        Map<UUID, List<Vehicle>> covering = vehicles.findAvailableVehicles().stream()
                .filter(vehicle -> active.containsKey(vehicle.getTravelPartnerId()))
                .filter(vehicle -> vehicle.coversLocation(latitude, longitude))
                .collect(Collectors.groupingBy(Vehicle::getTravelPartnerId, LinkedHashMap::new, Collectors.toList()));

        return new Coverage(active, covering);
    }

    private record Coverage(Map<UUID, TravelPartner> partners, Map<UUID, List<Vehicle>> vehicles) {
    }

    public record PartnerSummary(
            UUID id,
            String name,
            double rating,
            long reviewCount,
            int vehicleCount,
            List<String> categories,
            UUID coverVehicleId,
            UUID coverPhotoId
    ) {
    }

    public record VehicleView(Vehicle vehicle, Showcase showcase) {
    }

    public record PartnerDetail(PartnerSummary partner, List<VehicleView> vehicles) {
    }
}
