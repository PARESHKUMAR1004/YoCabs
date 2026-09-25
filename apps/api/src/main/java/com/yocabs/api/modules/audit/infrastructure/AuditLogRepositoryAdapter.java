package com.yocabs.api.modules.audit.infrastructure;

import com.yocabs.api.modules.audit.domain.AuditLog;
import com.yocabs.api.modules.audit.domain.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository jpa;

    public AuditLogRepositoryAdapter(AuditLogJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public AuditLog append(AuditLog entry) {
        return jpa.save(AuditLogEntity.fromDomain(entry)).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findRecent(int limit) {
        return jpa.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 500))))
                .stream().map(AuditLogEntity::toDomain).toList();
    }
}
