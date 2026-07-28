package com.shamsma.api.payment;

import java.math.BigDecimal;
import java.util.UUID;

/** Creates a hosted-checkout session with the payment gateway. */
public interface CmiCheckoutService {

  CmiCheckoutSession createSession(UUID paymentId, BigDecimal amount, String currency);
}
