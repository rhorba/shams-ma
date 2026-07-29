package com.shamsma.api.admin;

import com.shamsma.api.booking.AdminBookingOverviewFilter;
import com.shamsma.api.booking.AdminBookingOverviewRow;
import com.shamsma.api.booking.AdminBookingOverviewService;
import com.shamsma.api.booking.BookingStatus;
import com.shamsma.api.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/bookings")
class AdminBookingOverviewController {

  private static final DateTimeFormatter CSV_TIMESTAMP = DateTimeFormatter.ISO_INSTANT;

  private final AdminBookingOverviewService adminBookingOverviewService;

  AdminBookingOverviewController(AdminBookingOverviewService adminBookingOverviewService) {
    this.adminBookingOverviewService = adminBookingOverviewService;
  }

  @GetMapping
  Page<AdminBookingOverviewRow> list(
      @RequestParam(required = false) String bookingStatus,
      @RequestParam(required = false) String paymentStatus,
      @RequestParam(defaultValue = "false") boolean needsReviewOnly,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    AdminBookingOverviewFilter filter =
        parseFilter(bookingStatus, paymentStatus, needsReviewOnly, search);
    return adminBookingOverviewService.list(filter, PageRequest.of(page, size));
  }

  @GetMapping("/export")
  ResponseEntity<String> export(
      @RequestParam(required = false) String bookingStatus,
      @RequestParam(required = false) String paymentStatus,
      @RequestParam(defaultValue = "false") boolean needsReviewOnly,
      @RequestParam(required = false) String search) {
    AdminBookingOverviewFilter filter =
        parseFilter(bookingStatus, paymentStatus, needsReviewOnly, search);
    List<AdminBookingOverviewRow> rows = adminBookingOverviewService.listForExport(filter);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bookings.csv\"")
        .body(toCsv(rows));
  }

  private static AdminBookingOverviewFilter parseFilter(
      String bookingStatus, String paymentStatus, boolean needsReviewOnly, String search) {
    return new AdminBookingOverviewFilter(
        parseEnum(bookingStatus, BookingStatus.class),
        parseEnum(paymentStatus, PaymentStatus.class),
        needsReviewOnly,
        search);
  }

  private static <E extends Enum<E>> E parseEnum(String value, Class<E> type) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Enum.valueOf(type, value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid value: " + value);
    }
  }

  private static String toCsv(List<AdminBookingOverviewRow> rows) {
    StringBuilder csv = new StringBuilder();
    csv.append(
        "bookingId,bookingStatus,depositAmount,bookingCreatedAt,homeownerName,installerBusinessName,"
            + "paymentStatus,paymentAmount,cmiTransactionId,openFlagReason,openFlagExpectedAmount,"
            + "openFlagActualAmount\n");
    for (AdminBookingOverviewRow row : rows) {
      csv.append(csvField(row.bookingId()))
          .append(',')
          .append(csvField(row.bookingStatus()))
          .append(',')
          .append(csvField(row.depositAmount()))
          .append(',')
          .append(
              csvField(
                  row.bookingCreatedAt() == null
                      ? null
                      : CSV_TIMESTAMP.format(row.bookingCreatedAt())))
          .append(',')
          .append(csvField(row.homeownerName()))
          .append(',')
          .append(csvField(row.installerBusinessName()))
          .append(',')
          .append(csvField(row.paymentStatus()))
          .append(',')
          .append(csvField(row.paymentAmount()))
          .append(',')
          .append(csvField(row.cmiTransactionId()))
          .append(',')
          .append(csvField(row.openFlagReason()))
          .append(',')
          .append(csvField(row.openFlagExpectedAmount()))
          .append(',')
          .append(csvField(row.openFlagActualAmount()))
          .append('\n');
    }
    return csv.toString();
  }

  private static String csvField(Object value) {
    if (value == null) {
      return "";
    }
    String text = value instanceof BigDecimal amount ? amount.toPlainString() : value.toString();
    if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
      return "\"" + text.replace("\"", "\"\"") + "\"";
    }
    return text;
  }
}
