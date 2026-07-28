package com.shamsma.api.payment;

import java.math.BigDecimal;

/**
 * Our own webhook contract (mirrored by the mock trigger and expected of a future real CMI
 * integration) — no real CMI API docs exist to match against yet, see Epic 4 planning.
 */
public record CmiWebhookPayload(
    String transactionId, String status, BigDecimal amount, String currency) {}
