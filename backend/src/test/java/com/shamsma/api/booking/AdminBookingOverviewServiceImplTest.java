package com.shamsma.api.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.shamsma.api.homeowner.HomeownerService;
import com.shamsma.api.homeowner.HomeownerSummary;
import com.shamsma.api.installer.InstallerService;
import com.shamsma.api.installer.InstallerSummary;
import com.shamsma.api.installer.VerificationStatus;
import com.shamsma.api.payment.PaymentService;
import com.shamsma.api.payment.PaymentStatus;
import com.shamsma.api.payment.PaymentSummary;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminBookingOverviewServiceImplTest {

  @Mock private BookingRepository bookingRepository;
  @Mock private QuoteRequestRepository quoteRequestRepository;
  @Mock private HomeownerService homeownerService;
  @Mock private InstallerService installerService;
  @Mock private PaymentService paymentService;

  private AdminBookingOverviewServiceImpl service;

  private final UUID homeownerId = UUID.randomUUID();
  private final UUID installerId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new AdminBookingOverviewServiceImpl(
            bookingRepository,
            quoteRequestRepository,
            homeownerService,
            installerService,
            paymentService);
  }

  private void stubOneBookedRowWithNoFlag(Booking booking, QuoteRequest quoteRequest) {
    when(bookingRepository.findAll()).thenReturn(List.of(booking));
    when(quoteRequestRepository.findById(booking.getQuoteRequestId()))
        .thenReturn(Optional.of(quoteRequest));
    when(homeownerService.getSummary(homeownerId))
        .thenReturn(new HomeownerSummary(homeownerId, "Jane Doe", "0600000000"));
    when(installerService.getSummary(installerId))
        .thenReturn(
            new InstallerSummary(
                installerId, "SolarCo", "0700000000", VerificationStatus.APPROVED));
    when(paymentService.findByBookingIds(any()))
        .thenReturn(
            List.of(
                new PaymentSummary(
                    UUID.randomUUID(),
                    booking.getId(),
                    PaymentStatus.SUCCEEDED,
                    new BigDecimal("1000.00"),
                    "MAD",
                    "cmi-1",
                    null,
                    null,
                    null,
                    null)));
  }

  @Test
  void listReturnsAJoinedRowWithHomeownerInstallerAndPayment() {
    QuoteRequest quoteRequest = new QuoteRequest(homeownerId, installerId, null, null, null);
    Booking booking = new Booking(quoteRequest.getId(), new BigDecimal("1000.00"));
    booking.markBooked();
    stubOneBookedRowWithNoFlag(booking, quoteRequest);

    Page<AdminBookingOverviewRow> page =
        service.list(
            new AdminBookingOverviewFilter(null, null, false, null), PageRequest.of(0, 20));

    assertThat(page.getTotalElements()).isEqualTo(1);
    AdminBookingOverviewRow row = page.getContent().get(0);
    assertThat(row.homeownerName()).isEqualTo("Jane Doe");
    assertThat(row.installerBusinessName()).isEqualTo("SolarCo");
    assertThat(row.paymentStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
  }

  @Test
  void listFiltersByNeedsReviewOnly() {
    QuoteRequest quoteRequest = new QuoteRequest(homeownerId, installerId, null, null, null);
    Booking booking = new Booking(quoteRequest.getId(), new BigDecimal("1000.00"));
    stubOneBookedRowWithNoFlag(booking, quoteRequest);

    Page<AdminBookingOverviewRow> page =
        service.list(new AdminBookingOverviewFilter(null, null, true, null), PageRequest.of(0, 20));

    assertThat(page.getTotalElements()).isZero();
  }

  @Test
  void listFiltersBySearchTermOnHomeownerOrInstallerName() {
    QuoteRequest quoteRequest = new QuoteRequest(homeownerId, installerId, null, null, null);
    Booking booking = new Booking(quoteRequest.getId(), new BigDecimal("1000.00"));
    stubOneBookedRowWithNoFlag(booking, quoteRequest);

    Page<AdminBookingOverviewRow> matching =
        service.list(
            new AdminBookingOverviewFilter(null, null, false, "solarco"), PageRequest.of(0, 20));
    Page<AdminBookingOverviewRow> nonMatching =
        service.list(
            new AdminBookingOverviewFilter(null, null, false, "nomatch"), PageRequest.of(0, 20));

    assertThat(matching.getTotalElements()).isEqualTo(1);
    assertThat(nonMatching.getTotalElements()).isZero();
  }

  @Test
  void listForExportReturnsAllMatchingRowsUnpaginated() {
    QuoteRequest quoteRequest = new QuoteRequest(homeownerId, installerId, null, null, null);
    Booking booking = new Booking(quoteRequest.getId(), new BigDecimal("1000.00"));
    stubOneBookedRowWithNoFlag(booking, quoteRequest);

    List<AdminBookingOverviewRow> rows =
        service.listForExport(new AdminBookingOverviewFilter(null, null, false, null));

    assertThat(rows).hasSize(1);
  }
}
