package com.shamsma.api.notification;

import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logs instead of sending real email — explicit scope call for Epic 3 (the stories' acceptance
 * criteria literally say "receives an email notification"; SMTP env vars already exist and are
 * unused, so a real JavaMailSender-backed implementation is a drop-in swap behind this same
 * interface whenever that's prioritized).
 */
@Service
class LoggingNotificationServiceImpl implements NotificationService {

  private static final Logger log = LoggerFactory.getLogger(LoggingNotificationServiceImpl.class);

  @Override
  public void notifyNewQuoteRequest(UUID installerId, UUID quoteRequestId) {
    log.info("NOTIFY installer={} of new quote request={}", installerId, quoteRequestId);
  }

  @Override
  public void notifyQuoteResponse(
      UUID homeownerId, UUID quoteRequestId, boolean quoted, BigDecimal quoteAmount) {
    log.info(
        "NOTIFY homeowner={} that quote request={} was {}{}",
        homeownerId,
        quoteRequestId,
        quoted ? "QUOTED" : "DECLINED",
        quoted ? " amount=" + quoteAmount : "");
  }
}
