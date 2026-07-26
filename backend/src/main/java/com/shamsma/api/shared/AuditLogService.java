package com.shamsma.api.shared;

import java.util.UUID;

public interface AuditLogService {

  /** Records an audit entry per Security doc STRIDE "Repudiation" control. */
  void record(
      UUID actorUserId,
      String action,
      String entityType,
      UUID entityId,
      String previousStatus,
      String newStatus);
}
