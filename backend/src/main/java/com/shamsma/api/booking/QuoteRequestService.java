package com.shamsma.api.booking;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface QuoteRequestService {

  /** Creates one REQUESTED row per installer ID; each must be an APPROVED installer. */
  List<QuoteRequestSummary> requestQuotes(
      UUID homeownerId,
      List<UUID> installerIds,
      String message,
      BigDecimal roiEstimateKwh,
      BigDecimal roiPaybackYears);

  List<QuoteRequestSummary> listForHomeowner(UUID homeownerId);

  List<QuoteRequestSummary> listForInstaller(UUID installerId);

  /**
   * Ownership-checked: {@code installerId} must own the request, and it must still be REQUESTED.
   */
  QuoteRequestSummary respond(
      UUID installerId,
      UUID requestId,
      QuoteAction action,
      BigDecimal quoteAmount,
      String quoteNotes);

  /** Ownership-checked: {@code homeownerId} must own the request, and it must be QUOTED. */
  BookingResponse book(UUID homeownerId, UUID requestId);
}
