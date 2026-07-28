package com.shamsma.api.notification;

import java.math.BigDecimal;
import java.util.UUID;

/** Per PRD FR-11 — transactional notifications on quote/booking/certification/payment events. */
public interface NotificationService {

  void notifyNewQuoteRequest(UUID installerId, UUID quoteRequestId);

  void notifyQuoteResponse(
      UUID homeownerId, UUID quoteRequestId, boolean quoted, BigDecimal quoteAmount);

  void notifyPaymentSucceeded(UUID bookingId, UUID paymentId);
}
