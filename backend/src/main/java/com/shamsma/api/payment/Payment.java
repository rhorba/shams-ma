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

@Entity
@Table(name = "payments")
public class Payment {

  @Id @GeneratedValue @UuidGenerator private UUID id;

  @Column(name = "booking_id", nullable = false, unique = true)
  private UUID bookingId;

  @Column(name = "cmi_transaction_id", unique = true)
  private String cmiTransactionId;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private String currency;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private PaymentStatus status;

  @Column(name = "webhook_received_at")
  private Instant webhookReceivedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Payment() {}

  public Payment(UUID bookingId, BigDecimal amount, String currency) {
    this.bookingId = bookingId;
    this.amount = amount;
    this.currency = currency;
    this.status = PaymentStatus.PENDING;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getBookingId() {
    return bookingId;
  }

  public String getCmiTransactionId() {
    return cmiTransactionId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public void assignCmiTransactionId(String cmiTransactionId) {
    this.cmiTransactionId = cmiTransactionId;
    this.updatedAt = Instant.now();
  }

  public void markSucceeded() {
    this.status = PaymentStatus.SUCCEEDED;
    this.webhookReceivedAt = Instant.now();
    this.updatedAt = this.webhookReceivedAt;
  }

  public void markFailed() {
    this.status = PaymentStatus.FAILED;
    this.webhookReceivedAt = Instant.now();
    this.updatedAt = this.webhookReceivedAt;
  }

  /** Resets a PENDING/FAILED payment for a fresh checkout attempt (same row, new transaction). */
  public void resetForRetry() {
    this.status = PaymentStatus.PENDING;
    this.webhookReceivedAt = null;
    this.updatedAt = Instant.now();
  }
}
