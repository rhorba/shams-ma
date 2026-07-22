package com.shamsma.api.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
public class User {

  @Id @GeneratedValue @UuidGenerator private UUID id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private UserRole role;

  @Column(name = "mfa_secret")
  private String mfaSecret;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected User() {}

  public User(String email, String passwordHash, UserRole role) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public UserRole getRole() {
    return role;
  }

  public String getMfaSecret() {
    return mfaSecret;
  }

  public boolean isMfaEnrolled() {
    return mfaSecret != null;
  }

  public void setMfaSecret(String mfaSecret) {
    this.mfaSecret = mfaSecret;
    this.updatedAt = Instant.now();
  }
}
