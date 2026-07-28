package com.shamsma.api.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
    UUID id,
    UUID quoteRequestId,
    BookingStatus status,
    BigDecimal depositAmount,
    Instant createdAt) {}
