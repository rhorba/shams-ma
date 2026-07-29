package com.shamsma.api.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/** A system-raised flag on a payment that needs admin review (e.g. a webhook amount mismatch). */
@Entity
@Table(name = "payment_review_flags")
public class PaymentReviewFlag {

  @Id @GeneratedValue @UuidGenerator private UUID id;

  @Column(name = "payment_id", nullable = false)
  private UUID paymentId;

  @Column(nullable = false)
  private String reason;

  @Column(name = "expected_amount", nullable = false)
  private BigDecimal expectedAmount;

  @Column(name = "actual_amount", nullable = false)
  private BigDecimal actualAmount;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private PaymentReviewFlagStatus status;

  @Column(name = "resolution_note")
  private String resolutionNote;

  @Column(name = "resolved_by_user_id")
  private UUID resolvedByUserId;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected PaymentReviewFlag() {}

  public PaymentReviewFlag(
      UUID paymentId, String reason, BigDecimal expectedAmount, BigDecimal actualAmount) {
    this.paymentId = paymentId;
    this.reason = reason;
    this.expectedAmount = expectedAmount;
    this.actualAmount = actualAmount;
    this.status = PaymentReviewFlagStatus.OPEN;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getPaymentId() {
    return paymentId;
  }

  public String getReason() {
    return reason;
  }

  public BigDecimal getExpectedAmount() {
    return expectedAmount;
  }

  public BigDecimal getActualAmount() {
    return actualAmount;
  }

  public PaymentReviewFlagStatus getStatus() {
    return status;
  }

  public String getResolutionNote() {
    return resolutionNote;
  }

  public UUID getResolvedByUserId() {
    return resolvedByUserId;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void resolve(UUID adminUserId, String note, PaymentReviewFlagStatus terminalStatus) {
    this.status = terminalStatus;
    this.resolvedByUserId = adminUserId;
    this.resolutionNote = note;
    this.resolvedAt = Instant.now();
    this.updatedAt = this.resolvedAt;
  }
}
