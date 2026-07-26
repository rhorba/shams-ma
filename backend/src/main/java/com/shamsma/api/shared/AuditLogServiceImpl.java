package com.shamsma.api.shared;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class AuditLogServiceImpl implements AuditLogService {

  private final AuditLogRepository auditLogRepository;

  AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Override
  public void record(
      UUID actorUserId,
      String action,
      String entityType,
      UUID entityId,
      String previousStatus,
      String newStatus) {
    auditLogRepository.save(
        new AuditLog(actorUserId, action, entityType, entityId, previousStatus, newStatus));
  }
}
