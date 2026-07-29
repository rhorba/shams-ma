package com.shamsma.api.admin;

import com.shamsma.api.payment.PaymentReviewFlagService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/payment-flags")
class PaymentReviewFlagController {

  private final PaymentReviewFlagService paymentReviewFlagService;

  PaymentReviewFlagController(PaymentReviewFlagService paymentReviewFlagService) {
    this.paymentReviewFlagService = paymentReviewFlagService;
  }

  @PostMapping("/{id}/resolve")
  ResponseEntity<Void> resolve(
      Authentication authentication,
      @PathVariable UUID id,
      @RequestBody(required = false) NoteRequest body) {
    paymentReviewFlagService.resolve(id, currentUserId(authentication), note(body));
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{id}/dismiss")
  ResponseEntity<Void> dismiss(
      Authentication authentication,
      @PathVariable UUID id,
      @RequestBody(required = false) NoteRequest body) {
    paymentReviewFlagService.dismiss(id, currentUserId(authentication), note(body));
    return ResponseEntity.ok().build();
  }

  private static String note(NoteRequest body) {
    return body == null ? null : body.note();
  }

  private static UUID currentUserId(Authentication authentication) {
    return UUID.fromString((String) authentication.getPrincipal());
  }

  record NoteRequest(String note) {}
}
