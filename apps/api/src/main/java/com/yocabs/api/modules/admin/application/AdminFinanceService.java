package com.yocabs.api.modules.admin.application;

import com.yocabs.api.modules.admin.application.AdminFinanceViews.CancellationRow;
import com.yocabs.api.modules.admin.application.AdminFinanceViews.CancellationsView;
import com.yocabs.api.modules.admin.application.AdminFinanceViews.DateRange;
import com.yocabs.api.modules.admin.application.AdminFinanceViews.PaymentsView;
import com.yocabs.api.modules.admin.application.AdminFinanceViews.RefundsView;
import com.yocabs.api.modules.admin.infrastructure.AdminFinanceQuery;
import com.yocabs.api.modules.payment.domain.model.PaymentStatus;
import com.yocabs.api.shared.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
public class AdminFinanceService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final int DEFAULT_DAYS = 30;
    private static final long MAX_DAYS = 366;

    private final AdminFinanceQuery query;

    public AdminFinanceService(AdminFinanceQuery query) {
        this.query = query;
    }

    @Transactional(readOnly = true)
    public PaymentsView payments(Actor actor, LocalDate from, LocalDate to, PaymentStatus status, int limit) {

        actor.requireAdmin();

        DateRange range = range(from, to);
        Instant start = start(range);
        Instant end = endExclusive(range);
        String statusName = status == null ? null : status.name();

        return new PaymentsView(
                range,
                query.paymentsSummary(start, end, statusName),
                query.payments(start, end, statusName, clamp(limit))
        );
    }

    @Transactional(readOnly = true)
    public RefundsView refunds(Actor actor, LocalDate from, LocalDate to, int limit) {

        actor.requireAdmin();

        DateRange range = range(from, to);

        return new RefundsView(
                range,
                query.totalRefunded(start(range), endExclusive(range)),
                query.refunds(start(range), endExclusive(range), clamp(limit))
        );
    }

    @Transactional(readOnly = true)
    public CancellationsView cancellations(Actor actor, LocalDate from, LocalDate to, int limit) {

        actor.requireAdmin();

        DateRange range = range(from, to);
        java.util.List<CancellationRow> rows =
                query.cancellations(start(range), endExclusive(range), clamp(limit));

        return new CancellationsView(
                range,
                query.cancellationsByRole(start(range), endExclusive(range)),
                rows.stream().map(CancellationRow::refunded).reduce(BigDecimal.ZERO, BigDecimal::add),
                rows
        );
    }

    static DateRange range(LocalDate from, LocalDate to) {

        LocalDate end = to != null ? to : LocalDate.now(ZONE);
        LocalDate begin = from != null ? from : end.minusDays(DEFAULT_DAYS - 1L);

        if (begin.isAfter(end)) {
            throw new IllegalArgumentException("The start date cannot be after the end date");
        }

        if (ChronoUnit.DAYS.between(begin, end) > MAX_DAYS) {
            throw new IllegalArgumentException("The date range cannot exceed " + MAX_DAYS + " days");
        }

        return new DateRange(begin, end);
    }

    private static Instant start(DateRange range) {
        return range.from().atStartOfDay(ZONE).toInstant();
    }

    private static Instant endExclusive(DateRange range) {
        return range.to().plusDays(1).atStartOfDay(ZONE).toInstant();
    }

    private static int clamp(int limit) {
        return Math.max(1, Math.min(limit, 500));
    }
}
