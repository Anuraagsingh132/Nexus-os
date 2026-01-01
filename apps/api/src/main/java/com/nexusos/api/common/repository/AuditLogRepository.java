package com.nexusos.api.common.repository;

import com.nexusos.api.common.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
