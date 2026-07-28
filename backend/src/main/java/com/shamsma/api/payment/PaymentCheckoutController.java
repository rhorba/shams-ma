package com.shamsma.api.payment;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Self-service endpoint for the authenticated homeowner's own bookings. */
@RestController
@RequestMapping("/api/v1/homeowner/bookings")
class PaymentCheckoutController {

  private final PaymentService paymentService;

  PaymentCheckoutController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping("/{id}/checkout")
  ResponseEntity<CheckoutResponse> checkout(Authentication authentication, @PathVariable UUID id) {
    CheckoutResponse response = paymentService.initiateCheckout(currentUserId(authentication), id);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  private static UUID currentUserId(Authentication authentication) {
    return UUID.fromString((String) authentication.getPrincipal());
  }
}
