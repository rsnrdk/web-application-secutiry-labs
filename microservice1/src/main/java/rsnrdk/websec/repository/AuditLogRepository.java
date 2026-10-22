package rsnrdk.websec.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rsnrdk.websec.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}