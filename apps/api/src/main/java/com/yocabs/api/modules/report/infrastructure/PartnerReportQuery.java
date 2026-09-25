package com.yocabs.api.modules.report.infrastructure;

import com.yocabs.api.modules.report.application.PartnerReports.CancellationRow;
import com.yocabs.api.modules.report.application.PartnerReports.DriverRow;
import com.yocabs.api.modules.report.application.PartnerReports.MonthlyEarnings;
import com.yocabs.api.modules.report.application.PartnerReports.TripRow;
import com.yocabs.api.modules.report.application.PartnerReports.VehicleRow;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read-only SQL aggregates; every query is scoped to one travel partner. */
@Component
public class PartnerReportQuery {

    private final NamedParameterJdbcTemplate jdbc;

    public PartnerReportQuery(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private MapSqlParameterSource params(UUID partnerId, LocalDate from, LocalDate to) {
        return new MapSqlParameterSource()
                .addValue("p", partnerId)
                .addValue("from", from)
                .addValue("to", to);
    }

    public EarningsTotals earningsTotals(UUID partnerId, LocalDate from, LocalDate to) {
        return jdbc.queryForObject(
                """
                select count(*) filter (where status = 'COMPLETED') completed,
                       count(*) filter (where status in ('CONFIRMED', 'IN_PROGRESS')) upcoming,
                       coalesce(sum(total_amount) filter (where status = 'COMPLETED'), 0) gross,
                       coalesce(sum(commission_amount) filter (where status = 'COMPLETED'), 0) commission,
                       coalesce(sum(token_amount) filter (where status = 'COMPLETED'), 0) token
                from bookings
                where travel_partner_id = :p and start_date between :from and :to
                """,
                params(partnerId, from, to),
                (rs, i) -> new EarningsTotals(
                        rs.getLong("completed"), rs.getLong("upcoming"),
                        rs.getBigDecimal("gross"), rs.getBigDecimal("commission"), rs.getBigDecimal("token"))
        );
    }

    public List<MonthlyEarnings> monthlyEarnings(UUID partnerId, LocalDate from, LocalDate to) {
        return jdbc.query(
                """
                select to_char(start_date, 'YYYY-MM') as month,
                       count(*) filter (where status = 'COMPLETED') trips,
                       coalesce(sum(total_amount) filter (where status = 'COMPLETED'), 0) gross,
                       coalesce(sum(commission_amount) filter (where status = 'COMPLETED'), 0) commission
                from bookings
                where travel_partner_id = :p and start_date between :from and :to
                group by 1 order by 1
                """,
                params(partnerId, from, to),
                (rs, i) -> new MonthlyEarnings(
                        rs.getString("month"), rs.getLong("trips"),
                        rs.getBigDecimal("gross"), rs.getBigDecimal("commission"))
        );
    }

    public BigDecimal walletBalance(UUID partnerId) {
        return jdbc.queryForObject(
                """
                select coalesce(sum(case when entry_type = 'CREDIT' then amount else -amount end), 0)
                from wallet_entries where travel_partner_id = :p
                """,
                new MapSqlParameterSource("p", partnerId),
                BigDecimal.class
        );
    }

    public Map<String, Long> countsByStatus(UUID partnerId, LocalDate from, LocalDate to) {
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbc.query(
                """
                select status, count(*) c from bookings
                where travel_partner_id = :p and start_date between :from and :to
                group by status order by status
                """,
                params(partnerId, from, to),
                rs -> {
                    counts.put(rs.getString("status"), rs.getLong("c"));
                }
        );
        return counts;
    }

    public List<TripRow> trips(UUID partnerId, LocalDate from, LocalDate to, int limit) {
        return jdbc.query(
                """
                select b.id, b.start_date, b.end_date, b.pickup_description, b.destination_description,
                       b.status, b.total_amount, v.registration_number, d.name driver_name
                from bookings b
                join vehicles v on v.id = b.vehicle_id
                left join drivers d on d.id = b.driver_id
                where b.travel_partner_id = :p and b.start_date between :from and :to
                order by b.start_date desc, b.created_at desc
                limit :limit
                """,
                params(partnerId, from, to).addValue("limit", limit),
                (rs, i) -> new TripRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("start_date", LocalDate.class),
                        rs.getObject("end_date", LocalDate.class),
                        rs.getString("pickup_description"),
                        rs.getString("destination_description"),
                        rs.getString("status"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("registration_number"),
                        rs.getString("driver_name"))
        );
    }

    public List<VehicleRow> vehicles(UUID partnerId, LocalDate from, LocalDate to) {
        return jdbc.query(
                """
                select v.id, v.registration_number, v.make, v.model, v.category, v.status,
                       count(b.id) filter (where b.status = 'COMPLETED') completed,
                       count(b.id) filter (where b.status = 'CANCELLED') cancelled,
                       coalesce(sum(b.total_amount) filter (where b.status = 'COMPLETED'), 0) revenue,
                       coalesce(sum(b.end_date - b.start_date + 1) filter (where b.status = 'COMPLETED'), 0) days
                from vehicles v
                left join bookings b on b.vehicle_id = v.id and b.start_date between :from and :to
                where v.travel_partner_id = :p
                group by v.id
                order by revenue desc, v.registration_number
                """,
                params(partnerId, from, to),
                (rs, i) -> new VehicleRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("registration_number"),
                        rs.getString("make") + " " + rs.getString("model"),
                        rs.getString("category"),
                        rs.getString("status"),
                        rs.getLong("completed"),
                        rs.getLong("cancelled"),
                        rs.getBigDecimal("revenue"),
                        rs.getLong("days"))
        );
    }

    public List<DriverRow> drivers(UUID partnerId, LocalDate from, LocalDate to) {
        return jdbc.query(
                """
                select d.id, d.name, d.status,
                       count(distinct b.id) filter (where b.status = 'COMPLETED') completed,
                       coalesce(avg(r.rating), 0) average_rating,
                       count(r.id) reviews
                from drivers d
                left join bookings b on b.driver_id = d.id and b.start_date between :from and :to
                left join reviews r on r.booking_id = b.id
                where d.travel_partner_id = :p
                group by d.id
                order by completed desc, d.name
                """,
                params(partnerId, from, to),
                (rs, i) -> new DriverRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("status"),
                        rs.getLong("completed"),
                        Math.round(rs.getDouble("average_rating") * 10.0) / 10.0,
                        rs.getLong("reviews"))
        );
    }

    public Map<String, Long> cancellationsByRole(UUID partnerId, LocalDate from, LocalDate to) {
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbc.query(
                """
                select coalesce(cancelled_by_role, 'UNKNOWN') role, count(*) c from bookings
                where travel_partner_id = :p and status = 'CANCELLED' and start_date between :from and :to
                group by 1 order by 1
                """,
                params(partnerId, from, to),
                rs -> {
                    counts.put(rs.getString("role"), rs.getLong("c"));
                }
        );
        return counts;
    }

    public List<CancellationRow> cancellations(UUID partnerId, LocalDate from, LocalDate to, int limit) {
        return jdbc.query(
                """
                select b.id, b.start_date, b.pickup_description, b.destination_description, b.total_amount,
                       b.cancellation_reason, b.cancelled_by_role, b.updated_at,
                       coalesce((select sum(t.amount)
                                 from payment_transactions t join payments p on p.id = t.payment_id
                                 where p.booking_id = b.id and t.type = 'REFUND' and t.status = 'SUCCEEDED'), 0) refunded
                from bookings b
                where b.travel_partner_id = :p and b.status = 'CANCELLED' and b.start_date between :from and :to
                order by b.updated_at desc
                limit :limit
                """,
                params(partnerId, from, to).addValue("limit", limit),
                (rs, i) -> {
                    Timestamp updated = rs.getTimestamp("updated_at");
                    return new CancellationRow(
                            rs.getObject("id", UUID.class),
                            rs.getObject("start_date", LocalDate.class),
                            rs.getString("pickup_description"),
                            rs.getString("destination_description"),
                            rs.getBigDecimal("total_amount"),
                            rs.getString("cancellation_reason"),
                            rs.getString("cancelled_by_role"),
                            updated == null ? null : updated.toInstant(),
                            rs.getBigDecimal("refunded"));
                }
        );
    }

    public record EarningsTotals(
            long completed,
            long upcoming,
            BigDecimal gross,
            BigDecimal commission,
            BigDecimal token
    ) {
    }
}
