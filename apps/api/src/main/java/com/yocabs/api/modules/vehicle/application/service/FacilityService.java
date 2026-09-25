package com.yocabs.api.modules.vehicle.application.service;

import com.yocabs.api.modules.audit.application.AuditService;
import com.yocabs.api.modules.vehicle.domain.model.Facility;
import com.yocabs.api.modules.vehicle.domain.repository.FacilityRepository;
import com.yocabs.api.shared.exception.ResourceNotFoundException;
import com.yocabs.api.shared.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FacilityService {

    private final FacilityRepository facilities;
    private final AuditService auditService;

    public FacilityService(FacilityRepository facilities, AuditService auditService) {
        this.facilities = facilities;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Facility> listActive() {
        return facilities.findAll(true);
    }

    @Transactional(readOnly = true)
    public List<Facility> listAll(Actor actor) {
        actor.requireAdmin();
        return facilities.findAll(false);
    }

    @Transactional
    public Facility create(Actor actor, String code, String name) {

        actor.requireAdmin();

        Facility facility = new Facility(code == null ? null : code.trim().toUpperCase(), name, true);

        if (facilities.findByCode(facility.code()).isPresent()) {
            throw new IllegalStateException("A facility with this code already exists");
        }

        Facility saved = facilities.save(facility);
        auditService.record(actor, "FACILITY_CREATED", "FACILITY", null, saved.code());
        return saved;
    }

    @Transactional
    public Facility setActive(Actor actor, String code, boolean active) {

        actor.requireAdmin();

        Facility facility =
                facilities.findByCode(code.toUpperCase())
                        .orElseThrow(() -> new ResourceNotFoundException("Facility not found: " + code));

        Facility saved = facilities.save(facility.withActive(active));
        auditService.record(actor, active ? "FACILITY_ACTIVATED" : "FACILITY_DEACTIVATED",
                "FACILITY", null, saved.code());
        return saved;
    }
}
