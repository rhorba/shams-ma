package com.shamsma.api.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
    UUID id,
    UUID quoteRequestId,
    BookingStatus status,
    BigDecimal depositAmount,
    Instant createdAt) {

  static BookingResponse from(Booking booking) {
    return new BookingResponse(
        booking.getId(),
        booking.getQuoteRequestId(),
        booking.getStatus(),
        booking.getDepositAmount(),
        booking.getCreatedAt());
  }
}
