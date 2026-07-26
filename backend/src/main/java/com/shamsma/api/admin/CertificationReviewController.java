package com.shamsma.api.admin;

import com.shamsma.api.installer.CertificationService;
import com.shamsma.api.installer.CertificationSummary;
import com.shamsma.api.installer.VerificationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/certifications")
class CertificationReviewController {

  private final CertificationService certificationService;

  CertificationReviewController(CertificationService certificationService) {
    this.certificationService = certificationService;
  }

  @GetMapping
  List<CertificationSummary> list(@RequestParam(defaultValue = "PENDING") String status) {
    VerificationStatus parsed = parseStatus(status);
    return certificationService.findByStatus(parsed);
  }

  @PostMapping("/{id}/approve")
  ResponseEntity<Void> approve(Authentication authentication, @PathVariable UUID id) {
    certificationService.approve(id, currentUserId(authentication));
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{id}/reject")
  ResponseEntity<Void> reject(Authentication authentication, @PathVariable UUID id) {
    certificationService.reject(id, currentUserId(authentication));
    return ResponseEntity.ok().build();
  }

  private static VerificationStatus parseStatus(String status) {
    try {
      return VerificationStatus.valueOf(status.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
    }
  }

  private static UUID currentUserId(Authentication authentication) {
    return UUID.fromString((String) authentication.getPrincipal());
  }
}
