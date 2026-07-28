package com.shamsma.api.booking;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class BookingServiceImpl implements BookingService {

  private final BookingRepository bookingRepository;
  private final QuoteRequestRepository quoteRequestRepository;

  BookingServiceImpl(
      BookingRepository bookingRepository, QuoteRequestRepository quoteRequestRepository) {
    this.bookingRepository = bookingRepository;
    this.quoteRequestRepository = quoteRequestRepository;
  }

  @Override
  public BookingResponse getOwnedBooking(UUID homeownerId, UUID bookingId) {
    Booking booking = findOrThrow(bookingId);
    QuoteRequest quoteRequest =
        quoteRequestRepository
            .findById(booking.getQuoteRequestId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote request not found"));
    if (!quoteRequest.getHomeownerId().equals(homeownerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your booking");
    }
    return BookingResponse.from(booking);
  }

  @Override
  @Transactional
  public void markPaid(UUID bookingId) {
    Booking booking = findOrThrow(bookingId);
    if (booking.getStatus() == BookingStatus.BOOKED) {
      return;
    }
    booking.markBooked();
    bookingRepository.save(booking);
  }

  private Booking findOrThrow(UUID bookingId) {
    return bookingRepository
        .findById(bookingId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
  }
}
