package com.shamsma.api.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cross-package view of a payment for admin oversight, including any currently-open review flag.
 */
public record PaymentSummary(
    UUID paymentId,
    UUID bookingId,
    PaymentStatus status,
    BigDecimal amount,
    String currency,
    String cmiTransactionId,
    UUID openFlagId,
    String openFlagReason,
    BigDecimal openFlagExpectedAmount,
    BigDecimal openFlagActualAmount) {}
