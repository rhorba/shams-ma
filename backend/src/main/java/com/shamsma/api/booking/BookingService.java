package com.shamsma.api.booking;

import java.util.UUID;

/**
 * Cross-package surface for the {@code booking} package's payment-related needs, kept separate from
 * {@link QuoteRequestService} (which owns the already-shipped request/respond/book flow) so that
 * adding payment support doesn't require touching that tested code.
 */
public interface BookingService {

  /** Ownership-checked: throws 404/403. */
  BookingResponse getOwnedBooking(UUID homeownerId, UUID bookingId);

  /** Flips PENDING_PAYMENT -> BOOKED. Idempotent no-op if already BOOKED. */
  void markPaid(UUID bookingId);
}
