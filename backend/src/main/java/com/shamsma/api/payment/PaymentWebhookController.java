package com.shamsma.api.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, signature-verified CMI payment confirmation callback. A missing signature header is
 * rejected with 400 automatically by Spring before this method even runs.
 */
@RestController
@RequestMapping("/api/v1/payments")
class PaymentWebhookController {

  private final PaymentService paymentService;

  PaymentWebhookController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping("/webhook")
  ResponseEntity<Void> handleWebhook(
      @RequestHeader("X-CMI-Signature") String signature, @RequestBody String rawBody) {
    paymentService.processWebhook(rawBody, signature);
    return ResponseEntity.ok().build();
  }
}
