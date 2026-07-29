package com.shamsma.api.booking;

import com.shamsma.api.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Flat, admin-facing view joining a booking to its quote request, parties, payment, and any open
 * review flag.
 */
public record AdminBookingOverviewRow(
    UUID bookingId,
    BookingStatus bookingStatus,
    BigDecimal depositAmount,
    Instant bookingCreatedAt,
    UUID homeownerUserId,
    String homeownerName,
    UUID installerUserId,
    String installerBusinessName,
    UUID paymentId,
    PaymentStatus paymentStatus,
    BigDecimal paymentAmount,
    String cmiTransactionId,
    UUID openFlagId,
    String openFlagReason,
    BigDecimal openFlagExpectedAmount,
    BigDecimal openFlagActualAmount) {}
