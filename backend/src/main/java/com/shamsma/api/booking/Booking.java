package com.shamsma.api.booking;

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
@Table(name = "bookings")
public class Booking {

  @Id @GeneratedValue @UuidGenerator private UUID id;

  @Column(name = "quote_request_id", nullable = false, unique = true)
  private UUID quoteRequestId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private BookingStatus status;

  @Column(name = "deposit_amount", nullable = false)
  private BigDecimal depositAmount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Booking() {}

  public Booking(UUID quoteRequestId, BigDecimal depositAmount) {
    this.quoteRequestId = quoteRequestId;
    this.status = BookingStatus.PENDING_PAYMENT;
    this.depositAmount = depositAmount;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getQuoteRequestId() {
    return quoteRequestId;
  }

  public BookingStatus getStatus() {
    return status;
  }

  public BigDecimal getDepositAmount() {
    return depositAmount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void markBooked() {
    this.status = BookingStatus.BOOKED;
    this.updatedAt = Instant.now();
  }
}
