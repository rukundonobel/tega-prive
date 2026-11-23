package com.tegaprive.common;

import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes audit log entries for financial and administrative operations.
 * Uses @Async + a separate transaction so an audit entry is never lost
 * because the calling transaction rolled back.
 *
 * Usage:
 *   auditService.log(adminId, "APPROVE_DRIVER", "Driver", driverId, "Driver approved");
 */
@Service
public class AuditService {

  private final AuditLogRepository repo;

  public AuditService(AuditLogRepository repo) {
    this.repo = repo;
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void log(UUID actorId, String action, String entityType, UUID entityId,
      String description) {
    AuditLog entry = new AuditLog();
    entry.setActorId(actorId);
    entry.setAction(action);
    entry.setEntityType(entityType);
    entry.setEntityId(entityId);
    entry.setDescription(description);
    repo.save(entry);
  }
}
