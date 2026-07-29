package com.shamsma.api.payment;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

  /** Ownership + PENDING_PAYMENT-status checked. Reuses the existing payment row on retry. */
  CheckoutResponse initiateCheckout(UUID homeownerId, UUID bookingId);

  /** Signature-verified, idempotent on an already-SUCCEEDED transaction ID. */
  void processWebhook(String rawBody, String signature);

  /** Cross-package lookup for admin oversight (e.g. bookings without a payment yet are omitted). */
  List<PaymentSummary> findByBookingIds(List<UUID> bookingIds);
}
