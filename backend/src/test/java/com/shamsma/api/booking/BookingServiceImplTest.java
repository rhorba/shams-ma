package com.shamsma.api.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

  @Mock private BookingRepository bookingRepository;
  @Mock private QuoteRequestRepository quoteRequestRepository;

  private BookingServiceImpl service;

  private final UUID homeownerId = UUID.randomUUID();
  private final UUID installerId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new BookingServiceImpl(bookingRepository, quoteRequestRepository);
  }

  @Test
  void getOwnedBookingReturnsBookingForItsHomeowner() {
    UUID quoteRequestId = UUID.randomUUID();
    QuoteRequest quoteRequest = new QuoteRequest(homeownerId, installerId, null, null, null);
    Booking booking = new Booking(quoteRequestId, new BigDecimal("5000.00"));
    when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));
    when(quoteRequestRepository.findById(quoteRequestId)).thenReturn(Optional.of(quoteRequest));

    BookingResponse response = service.getOwnedBooking(homeownerId, UUID.randomUUID());

    assertThat(response.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    assertThat(response.depositAmount()).isEqualByComparingTo("5000.00");
  }

  @Test
  void getOwnedBookingRejectsNonOwningHomeowner() {
    UUID quoteRequestId = UUID.randomUUID();
    QuoteRequest quoteRequest = new QuoteRequest(homeownerId, installerId, null, null, null);
    Booking booking = new Booking(quoteRequestId, BigDecimal.TEN);
    when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));
    when(quoteRequestRepository.findById(quoteRequestId)).thenReturn(Optional.of(quoteRequest));

    assertThatThrownBy(() -> service.getOwnedBooking(UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void getOwnedBookingRejectsUnknownBooking() {
    when(bookingRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getOwnedBooking(homeownerId, UUID.randomUUID()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void markPaidFlipsPendingPaymentToBooked() {
    Booking booking = new Booking(UUID.randomUUID(), BigDecimal.TEN);
    when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));

    service.markPaid(UUID.randomUUID());

    assertThat(booking.getStatus()).isEqualTo(BookingStatus.BOOKED);
    verify(bookingRepository).save(booking);
  }

  @Test
  void markPaidIsIdempotentWhenAlreadyBooked() {
    Booking booking = new Booking(UUID.randomUUID(), BigDecimal.TEN);
    booking.markBooked();
    when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));

    service.markPaid(UUID.randomUUID());

    verify(bookingRepository, never()).save(any());
  }
}
