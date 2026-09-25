package com.yocabs.api.modules.report.application;

import com.yocabs.api.modules.report.application.PartnerReports.CancellationsReport;
import com.yocabs.api.modules.report.application.PartnerReports.DateRange;
import com.yocabs.api.modules.report.application.PartnerReports.DriverRow;
import com.yocabs.api.modules.report.application.PartnerReports.EarningsReport;
import com.yocabs.api.modules.report.application.PartnerReports.TripsReport;
import com.yocabs.api.modules.report.application.PartnerReports.VehicleRow;
import com.yocabs.api.modules.report.infrastructure.PartnerReportQuery;
import com.yocabs.api.modules.report.infrastructure.PartnerReportQuery.EarningsTotals;
import com.yocabs.api.shared.security.Actor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class PartnerReportService {

    private static final int DEFAULT_DAYS = 30;
    private static final long MAX_DAYS = 366;

    private final PartnerReportQuery query;

    public PartnerReportService(PartnerReportQuery query) {
        this.query = query;
    }

    /** Money figures are for the owner (or an admin); operational reports are open to staff too. */
    @Transactional(readOnly = true)
    public EarningsReport earnings(Actor actor, UUID partnerId, LocalDate from, LocalDate to) {

        actor.requirePartnerAccess(partnerId);

        if (!actor.isAdmin() && !actor.isOwnerOfPartner()) {
            throw new AccessDeniedException("Only the partner owner can view earnings");
        }

        DateRange range = range(from, to);
        EarningsTotals totals = query.earningsTotals(partnerId, range.from(), range.to());

        return new EarningsReport(
                range,
                totals.completed(),
                totals.upcoming(),
                totals.gross(),
                totals.commission(),
                totals.gross().subtract(totals.commission()),
                totals.token(),
                query.walletBalance(partnerId),
                query.monthlyEarnings(partnerId, range.from(), range.to())
        );
    }

    @Transactional(readOnly = true)
    public TripsReport trips(Actor actor, UUID partnerId, LocalDate from, LocalDate to, int limit) {

        actor.requirePartnerAccess(partnerId);

        DateRange range = range(from, to);

        return new TripsReport(
                range,
                query.countsByStatus(partnerId, range.from(), range.to()),
                query.trips(partnerId, range.from(), range.to(), clamp(limit))
        );
    }

    @Transactional(readOnly = true)
    public List<VehicleRow> vehicles(Actor actor, UUID partnerId, LocalDate from, LocalDate to) {

        actor.requirePartnerAccess(partnerId);

        DateRange range = range(from, to);
        return query.vehicles(partnerId, range.from(), range.to());
    }

    @Transactional(readOnly = true)
    public List<DriverRow> drivers(Actor actor, UUID partnerId, LocalDate from, LocalDate to) {

        actor.requirePartnerAccess(partnerId);

        DateRange range = range(from, to);
        return query.drivers(partnerId, range.from(), range.to());
    }

    @Transactional(readOnly = true)
    public CancellationsReport cancellations(
            Actor actor, UUID partnerId, LocalDate from, LocalDate to, int limit
    ) {
        actor.requirePartnerAccess(partnerId);

        DateRange range = range(from, to);
        var rows = query.cancellations(partnerId, range.from(), range.to(), clamp(limit));

        return new CancellationsReport(
                range,
                query.cancellationsByRole(partnerId, range.from(), range.to()),
                rows.stream()
                        .map(row -> row.refunded())
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                rows
        );
    }

    /** Defaults to the last 30 days; ranges are capped at a year. */
    static DateRange range(LocalDate from, LocalDate to) {

        LocalDate end = to != null ? to : LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1L);

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("The start date cannot be after the end date");
        }

        if (ChronoUnit.DAYS.between(start, end) > MAX_DAYS) {
            throw new IllegalArgumentException("The date range cannot exceed " + MAX_DAYS + " days");
        }

        return new DateRange(start, end);
    }

    private static int clamp(int limit) {
        return Math.max(1, Math.min(limit, 500));
    }
}
