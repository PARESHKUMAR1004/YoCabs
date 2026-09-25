package com.yocabs.api.modules.audit.domain;

import java.util.List;

public interface AuditLogRepository {

    AuditLog append(AuditLog entry);

    List<AuditLog> findRecent(int limit);
}
