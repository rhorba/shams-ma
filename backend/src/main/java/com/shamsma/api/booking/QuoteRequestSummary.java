package com.shamsma.api.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record QuoteRequestSummary(
    UUID id,
    UUID installerId,
    String installerBusinessName,
    UUID homeownerId,
    String homeownerFullName,
    QuoteStatus status,
    String message,
    BigDecimal roiEstimateKwh,
    BigDecimal roiPaybackYears,
    BigDecimal quoteAmount,
    String quoteNotes,
    Instant respondedAt,
    Instant createdAt) {}
