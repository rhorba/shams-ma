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
@Table(name = "quote_requests")
public class QuoteRequest {

  @Id @GeneratedValue @UuidGenerator private UUID id;

  @Column(name = "homeowner_id", nullable = false)
  private UUID homeownerId;

  @Column(name = "installer_id", nullable = false)
  private UUID installerId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private QuoteStatus status;

  private String message;

  @Column(name = "roi_estimate_kwh")
  private BigDecimal roiEstimateKwh;

  @Column(name = "roi_payback_years")
  private BigDecimal roiPaybackYears;

  @Column(name = "quote_amount")
  private BigDecimal quoteAmount;

  @Column(name = "quote_notes")
  private String quoteNotes;

  @Column(name = "responded_at")
  private Instant respondedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected QuoteRequest() {}

  public QuoteRequest(
      UUID homeownerId,
      UUID installerId,
      String message,
      BigDecimal roiEstimateKwh,
      BigDecimal roiPaybackYears) {
    this.homeownerId = homeownerId;
    this.installerId = installerId;
    this.status = QuoteStatus.REQUESTED;
    this.message = message;
    this.roiEstimateKwh = roiEstimateKwh;
    this.roiPaybackYears = roiPaybackYears;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public UUID getHomeownerId() {
    return homeownerId;
  }

  public UUID getInstallerId() {
    return installerId;
  }

  public QuoteStatus getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public BigDecimal getRoiEstimateKwh() {
    return roiEstimateKwh;
  }

  public BigDecimal getRoiPaybackYears() {
    return roiPaybackYears;
  }

  public BigDecimal getQuoteAmount() {
    return quoteAmount;
  }

  public String getQuoteNotes() {
    return quoteNotes;
  }

  public Instant getRespondedAt() {
    return respondedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void markQuoted(BigDecimal quoteAmount, String quoteNotes) {
    this.status = QuoteStatus.QUOTED;
    this.quoteAmount = quoteAmount;
    this.quoteNotes = quoteNotes;
    this.respondedAt = Instant.now();
    this.updatedAt = this.respondedAt;
  }

  public void markDeclined(String quoteNotes) {
    this.status = QuoteStatus.DECLINED;
    this.quoteNotes = quoteNotes;
    this.respondedAt = Instant.now();
    this.updatedAt = this.respondedAt;
  }

  public void markBooked() {
    this.status = QuoteStatus.BOOKED;
    this.updatedAt = Instant.now();
  }
}
