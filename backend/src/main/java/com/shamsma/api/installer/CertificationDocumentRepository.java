package com.shamsma.api.installer;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CertificationDocumentRepository extends JpaRepository<CertificationDocument, UUID> {
  List<CertificationDocument> findByStatus(VerificationStatus status);
}
