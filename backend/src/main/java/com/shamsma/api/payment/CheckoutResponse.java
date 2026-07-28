package com.shamsma.api.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutResponse(
    UUID paymentId,
    String checkoutUrl,
    String cmiTransactionId,
    BigDecimal amount,
    String currency) {}
