package com.shamsma.api.installer;

import com.shamsma.api.shared.AuditLogService;
import com.shamsma.api.shared.storage.FileStorageService;
import com.shamsma.api.shared.storage.UploadValidator;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class CertificationServiceImpl implements CertificationService {

  private static final Duration VIEW_URL_TTL = Duration.ofMinutes(15);

  private final CertificationDocumentRepository certificationRepository;
  private final InstallerRepository installerRepository;
  private final FileStorageService fileStorageService;
  private final AuditLogService auditLogService;

  CertificationServiceImpl(
      CertificationDocumentRepository certificationRepository,
      InstallerRepository installerRepository,
      FileStorageService fileStorageService,
      AuditLogService auditLogService) {
    this.certificationRepository = certificationRepository;
    this.installerRepository = installerRepository;
    this.fileStorageService = fileStorageService;
    this.auditLogService = auditLogService;
  }

  @Override
  public CertificationSummary upload(UUID installerId, byte[] content) {
    Installer installer =
        installerRepository
            .findById(installerId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Installer not found"));

    String contentType = UploadValidator.detectContentType(content);
    String key =
        "certifications/%s/%s.%s"
            .formatted(installerId, UUID.randomUUID(), UploadValidator.extensionFor(contentType));
    fileStorageService.upload(key, content, contentType);

    CertificationDocument saved =
        certificationRepository.save(new CertificationDocument(installerId, key));
    return toSummary(saved, installer.getBusinessName());
  }

  @Override
  public List<CertificationSummary> findByStatus(VerificationStatus status) {
    return certificationRepository.findByStatus(status).stream()
        .map(
            doc ->
                toSummary(
                    doc,
                    installerRepository
                        .findById(doc.getInstallerId())
                        .map(Installer::getBusinessName)
                        .orElse("Unknown")))
        .toList();
  }

  @Override
  @Transactional
  public void approve(UUID certificationId, UUID adminUserId) {
    reviewAndFlip(
        certificationId, adminUserId, VerificationStatus.APPROVED, "CERTIFICATION_APPROVED");
  }

  @Override
  @Transactional
  public void reject(UUID certificationId, UUID adminUserId) {
    reviewAndFlip(
        certificationId, adminUserId, VerificationStatus.REJECTED, "CERTIFICATION_REJECTED");
  }

  private void reviewAndFlip(
      UUID certificationId, UUID adminUserId, VerificationStatus newStatus, String action) {
    CertificationDocument certification =
        certificationRepository
            .findById(certificationId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certification not found"));
    String previousStatus = certification.getStatus().name();

    certification.markReviewed(newStatus, adminUserId);
    certificationRepository.save(certification);

    Installer installer =
        installerRepository
            .findById(certification.getInstallerId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Installer not found"));
    installer.markVerificationStatus(newStatus);
    installerRepository.save(installer);

    auditLogService.record(
        adminUserId,
        action,
        "certification_document",
        certificationId,
        previousStatus,
        newStatus.name());
  }

  private CertificationSummary toSummary(CertificationDocument doc, String businessName) {
    return new CertificationSummary(
        doc.getId(),
        doc.getInstallerId(),
        businessName,
        doc.getStatus(),
        fileStorageService.presignedUrl(doc.getFileUrl(), VIEW_URL_TTL),
        doc.getUploadedAt());
  }
}
