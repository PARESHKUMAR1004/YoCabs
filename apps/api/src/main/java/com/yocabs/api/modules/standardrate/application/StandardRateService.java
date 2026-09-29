package com.yocabs.api.modules.standardrate.application;

import com.yocabs.api.modules.audit.application.AuditService;
import com.yocabs.api.modules.standardrate.domain.StandardRate;
import com.yocabs.api.modules.standardrate.domain.StandardRateRepository;
import com.yocabs.api.modules.vehicle.domain.model.VehicleCategory;
import com.yocabs.api.shared.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The reference fare YoCabs quotes for each vehicle type, admin-defined, so a traveller can judge
 * a partner's price against a benchmark rather than only against other partners.
 */
@Service
public class StandardRateService {

    private final StandardRateRepository rates;
    private final AuditService auditService;

    public StandardRateService(StandardRateRepository rates, AuditService auditService) {
        this.rates = rates;
        this.auditService = auditService;
    }

    /** Public: every category an admin has priced. A category with none set has no benchmark yet. */
    @Transactional(readOnly = true)
    public List<StandardRate> list() {
        return rates.findAll();
    }

    @Transactional(readOnly = true)
    public Map<VehicleCategory, StandardRate> byCategory() {
        return rates.findAllByCategory();
    }

    @Transactional
    public StandardRate setRate(Actor actor, VehicleCategory category, BigDecimal perKmRate) {

        actor.requireAdmin();

        StandardRate saved =
                rates.upsert(new StandardRate(category, perKmRate, actor.userId(), Instant.now()));

        auditService.record(actor, "STANDARD_RATE_SET", "STANDARD_RATE", null,
                category + " = " + perKmRate + "/km");

        return saved;
    }
}
