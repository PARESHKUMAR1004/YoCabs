package com.yocabs.api.modules.audit.application;

import com.yocabs.api.modules.audit.domain.AuditLog;
import com.yocabs.api.modules.audit.domain.AuditLogRepository;
import com.yocabs.api.shared.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogs;

    public AuditService(AuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    /** Joins the caller's transaction so the audit entry commits iff the action does. */
    @Transactional
    public void record(Actor actor, String action, String targetType, UUID targetId, String details) {
        auditLogs.append(
                new AuditLog(
                        UUID.randomUUID(),
                        actor.userId(),
                        actor.role(),
                        action,
                        targetType,
                        targetId,
                        details == null || details.length() <= 2000
                                ? details
                                : details.substring(0, 2000),
                        Instant.now()
                )
        );
    }

    @Transactional(readOnly = true)
    public List<AuditLog> recent(Actor actor, int limit) {
        actor.requireAdmin();
        return auditLogs.findRecent(limit);
    }
}
