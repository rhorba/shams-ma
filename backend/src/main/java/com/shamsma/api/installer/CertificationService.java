package com.shamsma.api.installer;

import java.util.List;
import java.util.UUID;

public interface CertificationService {

  /** Validates (magic bytes + size), stores the file, and records a PENDING certification row. */
  CertificationSummary upload(UUID installerId, byte[] content);

  List<CertificationSummary> findByStatus(VerificationStatus status);

  /** Approves the certification and flips the owning installer to APPROVED. */
  void approve(UUID certificationId, UUID adminUserId);

  /** Rejects the certification and flips the owning installer to REJECTED. */
  void reject(UUID certificationId, UUID adminUserId);
}
