package com.shamsma.api.payment;

import java.util.UUID;

public interface PaymentService {

  /** Ownership + PENDING_PAYMENT-status checked. Reuses the existing payment row on retry. */
  CheckoutResponse initiateCheckout(UUID homeownerId, UUID bookingId);

  /** Signature-verified, idempotent on an already-SUCCEEDED transaction ID. */
  void processWebhook(String rawBody, String signature);
}
