package com.shamsma.api.homeowner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Maps only the columns Story 0.3 (auth/registration) needs. {@code location} is intentionally
 * unmapped — it's NOT NULL in the schema but populated by Epic 1's geocoding step (see migration
 * V10 for the interim DB default).
 */
@Entity
@Table(name = "homeowners")
public class Homeowner {

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  private String phone;

  @Column(name = "address_text", nullable = false)
  private String addressText;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Homeowner() {}

  public Homeowner(UUID userId, String fullName, String phone, String addressText) {
    this.userId = userId;
    this.fullName = fullName;
    this.phone = phone;
    this.addressText = addressText;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getFullName() {
    return fullName;
  }

  public String getPhone() {
    return phone;
  }

  public String getAddressText() {
    return addressText;
  }
}
