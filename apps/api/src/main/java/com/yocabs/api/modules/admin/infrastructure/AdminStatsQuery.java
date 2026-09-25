package com.yocabs.api.modules.admin.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only aggregate counts for the admin dashboard (no domain behaviour). */
@Component
public class AdminStatsQuery {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Map<String, Long> partnersByStatus() {

        Map<String, Long> result = new LinkedHashMap<>();

        @SuppressWarnings("unchecked")
        List<Object[]> rows =
                entityManager
                        .createNativeQuery("select status, count(*) from travel_partners group by status")
                        .getResultList();

        for (Object[] row : rows) {
            result.put((String) row[0], ((Number) row[1]).longValue());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public long count(String table, String where) {
        // Table/where come from compile-time constants in AdminService only.
        return ((Number) entityManager
                .createNativeQuery("select count(*) from " + table + " where " + where)
                .getSingleResult()).longValue();
    }
}
