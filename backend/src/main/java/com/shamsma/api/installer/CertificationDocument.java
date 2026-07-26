package com.shamsma.api.installer;

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
@Table(name = "certification_documents")
public class CertificationDocument {

  @Id @GeneratedValue @UuidGenerator private UUID id;

  @Column(name = "installer_id", nullable = false)
  private UUID installerId;

  @Column(name = "file_url", nullable = false)
  private String fileUrl;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private VerificationStatus status;

  @Column(name = "reviewed_by")
  private UUID reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "uploaded_at", nullable = false, updatable = false)
  private Instant uploadedAt;

  protected CertificationDocument() {}

  public CertificationDocument(UUID installerId, String fileUrl) {
    this.installerId = installerId;
    this.fileUrl = fileUrl;
    this.status = VerificationStatus.PENDING;
    this.uploadedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getInstallerId() {
    return installerId;
  }

  public String getFileUrl() {
    return fileUrl;
  }

  public VerificationStatus getStatus() {
    return status;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }

  public void markReviewed(VerificationStatus status, UUID reviewedBy) {
    this.status = status;
    this.reviewedBy = reviewedBy;
    this.reviewedAt = Instant.now();
  }
}
