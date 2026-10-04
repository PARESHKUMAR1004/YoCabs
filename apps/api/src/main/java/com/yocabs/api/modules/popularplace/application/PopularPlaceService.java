package com.yocabs.api.modules.popularplace.application;

import com.yocabs.api.modules.audit.application.AuditService;
import com.yocabs.api.modules.popularplace.domain.PopularPlace;
import com.yocabs.api.modules.popularplace.domain.PopularPlaceRepository;
import com.yocabs.api.shared.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The destinations shown as quick picks on the tourist home screen: admin-curated by dropping a
 * pin on a map, rather than baked into the app as a static list.
 */
@Service
public class PopularPlaceService {

    private final PopularPlaceRepository places;
    private final AuditService auditService;

    public PopularPlaceService(PopularPlaceRepository places, AuditService auditService) {
        this.places = places;
        this.auditService = auditService;
    }

    /** Public: every place an admin has curated, in display order. */
    @Transactional(readOnly = true)
    public List<PopularPlace> list() {
        return places.findAllOrdered();
    }

    @Transactional
    public PopularPlace create(
            Actor actor, String name, String subtitle, double latitude, double longitude, int displayOrder
    ) {
        actor.requireAdmin();

        PopularPlace saved = places.save(new PopularPlace(
                UUID.randomUUID(), name, subtitle, latitude, longitude, displayOrder,
                actor.userId(), Instant.now()
        ));

        auditService.record(actor, "POPULAR_PLACE_CREATED", "POPULAR_PLACE", saved.id(), saved.name());

        return saved;
    }

    @Transactional
    public PopularPlace update(
            Actor actor, UUID id, String name, String subtitle, double latitude, double longitude, int displayOrder
    ) {
        actor.requireAdmin();

        places.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No popular place with that id"));

        PopularPlace saved = places.save(new PopularPlace(
                id, name, subtitle, latitude, longitude, displayOrder, actor.userId(), Instant.now()
        ));

        auditService.record(actor, "POPULAR_PLACE_UPDATED", "POPULAR_PLACE", saved.id(), saved.name());

        return saved;
    }

    @Transactional
    public void delete(Actor actor, UUID id) {
        actor.requireAdmin();

        PopularPlace place = places.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No popular place with that id"));

        places.deleteById(id);

        auditService.record(actor, "POPULAR_PLACE_DELETED", "POPULAR_PLACE", id, place.name());
    }
}
