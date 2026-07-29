package com.shamsma.api.booking;

import com.shamsma.api.homeowner.HomeownerService;
import com.shamsma.api.homeowner.HomeownerSummary;
import com.shamsma.api.installer.InstallerService;
import com.shamsma.api.installer.InstallerSummary;
import com.shamsma.api.payment.PaymentService;
import com.shamsma.api.payment.PaymentSummary;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class AdminBookingOverviewServiceImpl implements AdminBookingOverviewService {

  private final BookingRepository bookingRepository;
  private final QuoteRequestRepository quoteRequestRepository;
  private final HomeownerService homeownerService;
  private final InstallerService installerService;
  private final PaymentService paymentService;

  AdminBookingOverviewServiceImpl(
      BookingRepository bookingRepository,
      QuoteRequestRepository quoteRequestRepository,
      HomeownerService homeownerService,
      InstallerService installerService,
      PaymentService paymentService) {
    this.bookingRepository = bookingRepository;
    this.quoteRequestRepository = quoteRequestRepository;
    this.homeownerService = homeownerService;
    this.installerService = installerService;
    this.paymentService = paymentService;
  }

  @Override
  public Page<AdminBookingOverviewRow> list(AdminBookingOverviewFilter filter, Pageable pageable) {
    List<AdminBookingOverviewRow> rows = filteredRows(filter);
    int total = rows.size();
    int from = Math.min((int) pageable.getOffset(), total);
    int to = Math.min(from + pageable.getPageSize(), total);
    return new PageImpl<>(rows.subList(from, to), pageable, total);
  }

  @Override
  public List<AdminBookingOverviewRow> listForExport(AdminBookingOverviewFilter filter) {
    return filteredRows(filter);
  }

  private List<AdminBookingOverviewRow> filteredRows(AdminBookingOverviewFilter filter) {
    List<Booking> bookings = bookingRepository.findAll();
    List<UUID> bookingIds = bookings.stream().map(Booking::getId).toList();
    Map<UUID, PaymentSummary> paymentsByBookingId =
        paymentService.findByBookingIds(bookingIds).stream()
            .collect(Collectors.toMap(PaymentSummary::bookingId, Function.identity()));

    return bookings.stream()
        .map(booking -> toRow(booking, paymentsByBookingId.get(booking.getId())))
        .filter(row -> matches(row, filter))
        .sorted(Comparator.comparing(AdminBookingOverviewRow::bookingCreatedAt).reversed())
        .toList();
  }

  private AdminBookingOverviewRow toRow(Booking booking, PaymentSummary payment) {
    QuoteRequest quoteRequest =
        quoteRequestRepository
            .findById(booking.getQuoteRequestId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote request not found"));
    HomeownerSummary homeowner = homeownerService.getSummary(quoteRequest.getHomeownerId());
    InstallerSummary installer = installerService.getSummary(quoteRequest.getInstallerId());
    return new AdminBookingOverviewRow(
        booking.getId(),
        booking.getStatus(),
        booking.getDepositAmount(),
        booking.getCreatedAt(),
        homeowner.userId(),
        homeowner.fullName(),
        installer.userId(),
        installer.businessName(),
        payment == null ? null : payment.paymentId(),
        payment == null ? null : payment.status(),
        payment == null ? null : payment.amount(),
        payment == null ? null : payment.cmiTransactionId(),
        payment == null ? null : payment.openFlagId(),
        payment == null ? null : payment.openFlagReason(),
        payment == null ? null : payment.openFlagExpectedAmount(),
        payment == null ? null : payment.openFlagActualAmount());
  }

  private static boolean matches(AdminBookingOverviewRow row, AdminBookingOverviewFilter filter) {
    if (filter.bookingStatus() != null && row.bookingStatus() != filter.bookingStatus()) {
      return false;
    }
    if (filter.paymentStatus() != null && row.paymentStatus() != filter.paymentStatus()) {
      return false;
    }
    if (filter.needsReviewOnly() && row.openFlagId() == null) {
      return false;
    }
    if (filter.search() != null && !filter.search().isBlank()) {
      String needle = filter.search().toLowerCase();
      boolean matchesHomeowner =
          row.homeownerName() != null && row.homeownerName().toLowerCase().contains(needle);
      boolean matchesInstaller =
          row.installerBusinessName() != null
              && row.installerBusinessName().toLowerCase().contains(needle);
      if (!matchesHomeowner && !matchesInstaller) {
        return false;
      }
    }
    return true;
  }
}
