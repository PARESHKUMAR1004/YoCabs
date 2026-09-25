package com.yocabs.api.modules.audit.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogJpaRepository
        extends JpaRepository<AuditLogEntity, UUID> {

    List<AuditLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
