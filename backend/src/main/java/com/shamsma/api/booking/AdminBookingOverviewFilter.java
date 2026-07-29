package com.shamsma.api.booking;

import com.shamsma.api.payment.PaymentStatus;

public record AdminBookingOverviewFilter(
    BookingStatus bookingStatus,
    PaymentStatus paymentStatus,
    boolean needsReviewOnly,
    String search) {}
