package com.shamsma.api.installer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Maps only the columns Story 0.3 (auth/registration) needs. {@code base_location} and {@code
 * coverage_radius_km} are nullable in the schema and set later, in Epic 1's coverage-zone story —
 * intentionally unmapped here.
 */
@Entity
@Table(name = "installers")
public class Installer {

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "business_name", nullable = false)
  private String businessName;

  private String phone;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "verification_status", nullable = false)
  private VerificationStatus verificationStatus;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Installer() {}

  public Installer(UUID userId, String businessName, String phone) {
    this.userId = userId;
    this.businessName = businessName;
    this.phone = phone;
    this.verificationStatus = VerificationStatus.PENDING;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getBusinessName() {
    return businessName;
  }

  public String getPhone() {
    return phone;
  }

  public VerificationStatus getVerificationStatus() {
    return verificationStatus;
  }
}
