package com.yocabs.api.modules.admin.infrastructure;

import com.yocabs.api.modules.admin.application.AdminFinanceViews.CancellationRow;
import com.yocabs.api.modules.admin.application.AdminFinanceViews.PaymentRow;
import com.yocabs.api.modules.admin.application.AdminFinanceViews.PaymentsSummary;
import com.yocabs.api.modules.admin.application.AdminFinanceViews.RefundRow;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read-only SQL for the admin finance screens. Time bounds are absolute instants. */
@Component
public class AdminFinanceQuery {

    private static final String PAID = "('SUCCEEDED', 'PARTIALLY_REFUNDED', 'REFUNDED')";

    private final NamedParameterJdbcTemplate jdbc;

    public AdminFinanceQuery(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private MapSqlParameterSource bounds(Instant from, Instant toExclusive) {
        return new MapSqlParameterSource()
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(toExclusive));
    }

    public PaymentsSummary paymentsSummary(Instant from, Instant toExclusive, String status) {

        String statusClause = status == null ? "" : " and p.status = :status";

        MapSqlParameterSource params = bounds(from, toExclusive);
        if (status != null) {
            params.addValue("status", status);
        }

        return jdbc.queryForObject(
                "select count(*) filter (where p.status in " + PAID + ") paid, "
                        + "coalesce(sum(p.amount) filter (where p.status in " + PAID + "), 0) collected, "
                        + "coalesce(sum(p.refunded_amount), 0) refunded "
                        + "from payments p where p.created_at >= :from and p.created_at < :to" + statusClause,
                params,
                (rs, i) -> {
                    BigDecimal collected = rs.getBigDecimal("collected");
                    BigDecimal refunded = rs.getBigDecimal("refunded");
                    return new PaymentsSummary(
                            rs.getLong("paid"), collected, refunded, collected.subtract(refunded));
                }
        );
    }

    public List<PaymentRow> payments(Instant from, Instant toExclusive, String status, int limit) {

        String statusClause = status == null ? "" : " and p.status = :status";

        MapSqlParameterSource params = bounds(from, toExclusive).addValue("limit", limit);
        if (status != null) {
            params.addValue("status", status);
        }

        return jdbc.query(
                "select p.id, p.booking_id, b.travel_partner_id, b.tourist_id, p.amount, p.currency, "
                        + "p.status, p.refunded_amount, p.gateway, p.created_at "
                        + "from payments p join bookings b on b.id = p.booking_id "
                        + "where p.created_at >= :from and p.created_at < :to" + statusClause
                        + " order by p.created_at desc limit :limit",
                params,
                (rs, i) -> new PaymentRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("booking_id", UUID.class),
                        rs.getObject("travel_partner_id", UUID.class),
                        rs.getObject("tourist_id", UUID.class),
                        rs.getBigDecimal("amount"),
                        rs.getString("currency"),
                        rs.getString("status"),
                        rs.getBigDecimal("refunded_amount"),
                        rs.getString("gateway"),
                        rs.getTimestamp("created_at").toInstant())
        );
    }

    public List<RefundRow> refunds(Instant from, Instant toExclusive, int limit) {
        return jdbc.query(
                """
                select t.id, t.payment_id, p.booking_id, b.travel_partner_id, t.amount,
                       b.cancellation_reason, b.cancelled_by_role, t.created_at
                from payment_transactions t
                join payments p on p.id = t.payment_id
                join bookings b on b.id = p.booking_id
                where t.type = 'REFUND' and t.status = 'SUCCEEDED'
                  and t.created_at >= :from and t.created_at < :to
                order by t.created_at desc
                limit :limit
                """,
                bounds(from, toExclusive).addValue("limit", limit),
                (rs, i) -> new RefundRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("payment_id", UUID.class),
                        rs.getObject("booking_id", UUID.class),
                        rs.getObject("travel_partner_id", UUID.class),
                        rs.getBigDecimal("amount"),
                        rs.getString("cancellation_reason"),
                        rs.getString("cancelled_by_role"),
                        rs.getTimestamp("created_at").toInstant())
        );
    }

    public BigDecimal totalRefunded(Instant from, Instant toExclusive) {
        return jdbc.queryForObject(
                """
                select coalesce(sum(amount), 0) from payment_transactions
                where type = 'REFUND' and status = 'SUCCEEDED' and created_at >= :from and created_at < :to
                """,
                bounds(from, toExclusive),
                BigDecimal.class
        );
    }

    public Map<String, Long> cancellationsByRole(Instant from, Instant toExclusive) {
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbc.query(
                """
                select coalesce(cancelled_by_role, 'UNKNOWN') role, count(*) c from bookings
                where status = 'CANCELLED' and updated_at >= :from and updated_at < :to
                group by 1 order by 1
                """,
                bounds(from, toExclusive),
                rs -> {
                    counts.put(rs.getString("role"), rs.getLong("c"));
                }
        );
        return counts;
    }

    public List<CancellationRow> cancellations(Instant from, Instant toExclusive, int limit) {
        return jdbc.query(
                """
                select b.id, b.travel_partner_id, b.tourist_id, b.start_date, b.total_amount, b.token_amount,
                       b.cancellation_reason, b.cancelled_by_role, b.updated_at,
                       coalesce((select sum(t.amount)
                                 from payment_transactions t join payments p on p.id = t.payment_id
                                 where p.booking_id = b.id and t.type = 'REFUND' and t.status = 'SUCCEEDED'), 0) refunded
                from bookings b
                where b.status = 'CANCELLED' and b.updated_at >= :from and b.updated_at < :to
                order by b.updated_at desc
                limit :limit
                """,
                bounds(from, toExclusive).addValue("limit", limit),
                (rs, i) -> new CancellationRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("travel_partner_id", UUID.class),
                        rs.getObject("tourist_id", UUID.class),
                        rs.getObject("start_date", LocalDate.class),
                        rs.getBigDecimal("total_amount"),
                        rs.getBigDecimal("token_amount"),
                        rs.getString("cancellation_reason"),
                        rs.getString("cancelled_by_role"),
                        rs.getTimestamp("updated_at").toInstant(),
                        rs.getBigDecimal("refunded"))
        );
    }
}
