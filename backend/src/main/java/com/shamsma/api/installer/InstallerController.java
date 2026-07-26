package com.shamsma.api.installer;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Self-service endpoints for the authenticated installer's own profile. */
@RestController
@RequestMapping("/api/v1/installer")
class InstallerController {

  private final InstallerService installerService;
  private final CertificationService certificationService;

  InstallerController(
      InstallerService installerService, CertificationService certificationService) {
    this.installerService = installerService;
    this.certificationService = certificationService;
  }

  @PutMapping("/coverage-zone")
  ResponseEntity<CoverageZoneResponse> setCoverageZone(
      Authentication authentication, @Valid @RequestBody CoverageZoneRequest request) {
    CoverageZoneResponse response =
        installerService.setCoverageZone(
            currentUserId(authentication), request.addressText(), request.radiusKm());
    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/certifications", consumes = "multipart/form-data")
  ResponseEntity<CertificationSummary> uploadCertification(
      Authentication authentication, @RequestParam("file") MultipartFile file) {
    byte[] content;
    try {
      content = file.getBytes();
    } catch (java.io.IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
    }
    CertificationSummary summary =
        certificationService.upload(currentUserId(authentication), content);
    return ResponseEntity.status(HttpStatus.CREATED).body(summary);
  }

  private static UUID currentUserId(Authentication authentication) {
    return UUID.fromString((String) authentication.getPrincipal());
  }
}
