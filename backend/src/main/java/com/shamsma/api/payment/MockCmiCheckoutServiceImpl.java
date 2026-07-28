package com.shamsma.api.payment;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Stands in for a real CMI SDK/API call — this project has no real CMI merchant credentials (see
 * Epic 4 planning in .logs/sessions.md). Everything downstream of this call (signature
 * verification, idempotency, the payment/booking state machine in {@link PaymentServiceImpl}) is
 * fully real regardless of which {@link CmiCheckoutService} is wired in; only this outbound "create
 * the hosted session" call is a stand-in, swappable behind this interface once real merchant
 * credentials exist.
 */
@Service
class MockCmiCheckoutServiceImpl implements CmiCheckoutService {

  @Override
  public CmiCheckoutSession createSession(UUID paymentId, BigDecimal amount, String currency) {
    String transactionId = "MOCK-" + UUID.randomUUID();
    return new CmiCheckoutSession(transactionId, "/api/v1/mock-cmi/" + transactionId);
  }
}
