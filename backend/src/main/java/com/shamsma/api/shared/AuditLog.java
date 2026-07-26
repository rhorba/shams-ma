package com.shamsma.api.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "audit_log")
public class AuditLog {

  @Id @GeneratedValue @UuidGenerator private UUID id;

  @Column(name = "actor_user_id", nullable = false)
  private UUID actorUserId;

  @Column(nullable = false)
  private String action;

  @Column(name = "entity_type", nullable = false)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private UUID entityId;

  @Column(name = "previous_status")
  private String previousStatus;

  @Column(name = "new_status")
  private String newStatus;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected AuditLog() {}

  public AuditLog(
      UUID actorUserId,
      String action,
      String entityType,
      UUID entityId,
      String previousStatus,
      String newStatus) {
    this.actorUserId = actorUserId;
    this.action = action;
    this.entityType = entityType;
    this.entityId = entityId;
    this.previousStatus = previousStatus;
    this.newStatus = newStatus;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }
}
