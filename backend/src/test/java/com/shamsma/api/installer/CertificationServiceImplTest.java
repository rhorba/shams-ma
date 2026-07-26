package com.shamsma.api.installer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shamsma.api.shared.AuditLogService;
import com.shamsma.api.shared.storage.FileStorageService;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CertificationServiceImplTest {

  private static final byte[] PDF_BYTES = {0x25, 0x50, 0x44, 0x46, 0x01, 0x02, 0x03};

  @Mock private CertificationDocumentRepository certificationRepository;
  @Mock private InstallerRepository installerRepository;
  @Mock private FileStorageService fileStorageService;
  @Mock private AuditLogService auditLogService;

  private CertificationServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new CertificationServiceImpl(
            certificationRepository, installerRepository, fileStorageService, auditLogService);
  }

  @Test
  void uploadValidatesStoresAndPersistsPending() {
    UUID installerId = UUID.randomUUID();
    Installer installer = new Installer(installerId, "Solaire Atlas", "+212600000000");
    when(installerRepository.findById(installerId)).thenReturn(Optional.of(installer));
    when(certificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(fileStorageService.presignedUrl(anyString(), any()))
        .thenReturn(URI.create("https://signed"));

    CertificationSummary summary = service.upload(installerId, PDF_BYTES);

    assertThat(summary.installerId()).isEqualTo(installerId);
    assertThat(summary.businessName()).isEqualTo("Solaire Atlas");
    assertThat(summary.status()).isEqualTo(VerificationStatus.PENDING);
    verify(fileStorageService).upload(anyString(), eq(PDF_BYTES), eq("application/pdf"));
  }

  @Test
  void uploadRejectsUnknownInstaller() {
    when(installerRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.upload(UUID.randomUUID(), PDF_BYTES))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void uploadRejectsInvalidFileContent() {
    UUID installerId = UUID.randomUUID();
    when(installerRepository.findById(installerId))
        .thenReturn(Optional.of(new Installer(installerId, "Solaire Atlas", null)));

    assertThatThrownBy(() -> service.upload(installerId, "not a pdf".getBytes()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unsupported file type");
  }

  @Test
  void approveFlipsCertificationAndInstallerAndWritesAuditLog() {
    UUID certId = UUID.randomUUID();
    UUID installerId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    CertificationDocument cert = new CertificationDocument(installerId, "certifications/x/y.pdf");
    Installer installer = new Installer(installerId, "Solaire Atlas", null);
    when(certificationRepository.findById(certId)).thenReturn(Optional.of(cert));
    when(installerRepository.findById(installerId)).thenReturn(Optional.of(installer));

    service.approve(certId, adminId);

    assertThat(cert.getStatus()).isEqualTo(VerificationStatus.APPROVED);
    assertThat(installer.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);
    ArgumentCaptor<String> previousStatus = ArgumentCaptor.forClass(String.class);
    verify(auditLogService)
        .record(
            eq(adminId),
            eq("CERTIFICATION_APPROVED"),
            eq("certification_document"),
            eq(certId),
            previousStatus.capture(),
            eq("APPROVED"));
    assertThat(previousStatus.getValue()).isEqualTo("PENDING");
  }

  @Test
  void rejectFlipsCertificationAndInstallerToRejected() {
    UUID certId = UUID.randomUUID();
    UUID installerId = UUID.randomUUID();
    CertificationDocument cert = new CertificationDocument(installerId, "certifications/x/y.pdf");
    Installer installer = new Installer(installerId, "Solaire Atlas", null);
    when(certificationRepository.findById(certId)).thenReturn(Optional.of(cert));
    when(installerRepository.findById(installerId)).thenReturn(Optional.of(installer));

    service.reject(certId, UUID.randomUUID());

    assertThat(cert.getStatus()).isEqualTo(VerificationStatus.REJECTED);
    assertThat(installer.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
  }

  @Test
  void approveRejectsUnknownCertification() {
    when(certificationRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.approve(UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void findByStatusMapsToSummariesWithBusinessNameAndSignedUrl() {
    UUID installerId = UUID.randomUUID();
    CertificationDocument cert = new CertificationDocument(installerId, "certifications/x/y.pdf");
    Installer installer = new Installer(installerId, "Solaire Atlas", null);
    when(certificationRepository.findByStatus(VerificationStatus.PENDING))
        .thenReturn(List.of(cert));
    when(installerRepository.findById(installerId)).thenReturn(Optional.of(installer));
    when(fileStorageService.presignedUrl(eq("certifications/x/y.pdf"), eq(Duration.ofMinutes(15))))
        .thenReturn(URI.create("https://signed/x/y.pdf"));

    List<CertificationSummary> summaries = service.findByStatus(VerificationStatus.PENDING);

    assertThat(summaries).hasSize(1);
    assertThat(summaries.get(0).businessName()).isEqualTo("Solaire Atlas");
    assertThat(summaries.get(0).viewUrl()).isEqualTo(URI.create("https://signed/x/y.pdf"));
  }
}
