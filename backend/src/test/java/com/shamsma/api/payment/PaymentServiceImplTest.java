package com.shamsma.api.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shamsma.api.booking.BookingResponse;
import com.shamsma.api.booking.BookingService;
import com.shamsma.api.booking.BookingStatus;
import com.shamsma.api.notification.NotificationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

  private static final UUID HOMEOWNER_ID = UUID.randomUUID();
  private static final UUID BOOKING_ID = UUID.randomUUID();
  private static final String SECRET = "test-secret";

  @Mock private PaymentRepository paymentRepository;
  @Mock private BookingService bookingService;
  @Mock private CmiCheckoutService cmiCheckoutService;
  @Mock private NotificationService notificationService;

  private CmiSignatureService cmiSignatureService;
  private PaymentServiceImpl service;

  @BeforeEach
  void setUp() {
    cmiSignatureService = new CmiSignatureService(SECRET);
    service =
        new PaymentServiceImpl(
            paymentRepository,
            bookingService,
            cmiCheckoutService,
            cmiSignatureService,
            notificationService,
            new JsonMapper());
  }

  private BookingResponse pendingPaymentBooking() {
    return new BookingResponse(
        BOOKING_ID,
        UUID.randomUUID(),
        BookingStatus.PENDING_PAYMENT,
        new BigDecimal("5000.00"),
        Instant.now());
  }

  @Test
  void initiateCheckoutCreatesAPaymentRowAndSession() {
    when(bookingService.getOwnedBooking(HOMEOWNER_ID, BOOKING_ID))
        .thenReturn(pendingPaymentBooking());
    when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.empty());
    when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(cmiCheckoutService.createSession(any(), any(), any()))
        .thenReturn(new CmiCheckoutSession("MOCK-1", "/api/v1/mock-cmi/MOCK-1"));

    CheckoutResponse response = service.initiateCheckout(HOMEOWNER_ID, BOOKING_ID);

    assertThat(response.cmiTransactionId()).isEqualTo("MOCK-1");
    assertThat(response.amount()).isEqualByComparingTo("5000.00");
  }

  @Test
  void initiateCheckoutRejectsABookingThatIsNotAwaitingPayment() {
    when(bookingService.getOwnedBooking(HOMEOWNER_ID, BOOKING_ID))
        .thenReturn(
            new BookingResponse(
                BOOKING_ID,
                UUID.randomUUID(),
                BookingStatus.BOOKED,
                BigDecimal.TEN,
                Instant.now()));

    assertThatThrownBy(() -> service.initiateCheckout(HOMEOWNER_ID, BOOKING_ID))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.BAD_REQUEST);
    verify(cmiCheckoutService, never()).createSession(any(), any(), any());
  }

  @Test
  void initiateCheckoutRejectsAnAlreadyPaidBooking() {
    when(bookingService.getOwnedBooking(HOMEOWNER_ID, BOOKING_ID))
        .thenReturn(pendingPaymentBooking());
    Payment succeeded = new Payment(BOOKING_ID, new BigDecimal("5000.00"), "MAD");
    succeeded.markSucceeded();
    when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.of(succeeded));

    assertThatThrownBy(() -> service.initiateCheckout(HOMEOWNER_ID, BOOKING_ID))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already been paid");
  }

  @Test
  void initiateCheckoutRetryReusesTheExistingFailedPaymentRow() {
    when(bookingService.getOwnedBooking(HOMEOWNER_ID, BOOKING_ID))
        .thenReturn(pendingPaymentBooking());
    Payment failed = new Payment(BOOKING_ID, new BigDecimal("5000.00"), "MAD");
    failed.assignCmiTransactionId("MOCK-OLD");
    failed.markFailed();
    when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.of(failed));
    when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(cmiCheckoutService.createSession(any(), any(), any()))
        .thenReturn(new CmiCheckoutSession("MOCK-NEW", "/api/v1/mock-cmi/MOCK-NEW"));

    CheckoutResponse response = service.initiateCheckout(HOMEOWNER_ID, BOOKING_ID);

    assertThat(response.cmiTransactionId()).isEqualTo("MOCK-NEW");
    assertThat(failed.getStatus()).isEqualTo(PaymentStatus.PENDING);
    verify(paymentRepository).save(failed);
  }

  @Test
  void initiateCheckoutRecoversFromADoubleClickRaceOnTheUniqueConstraint() {
    when(bookingService.getOwnedBooking(HOMEOWNER_ID, BOOKING_ID))
        .thenReturn(pendingPaymentBooking());
    Payment winnerRow = new Payment(BOOKING_ID, new BigDecimal("5000.00"), "MAD");
    when(paymentRepository.findByBookingId(BOOKING_ID))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(winnerRow));
    when(paymentRepository.save(any()))
        .thenThrow(new DataIntegrityViolationException("duplicate"))
        .thenAnswer(inv -> inv.getArgument(0));
    when(cmiCheckoutService.createSession(any(), any(), any()))
        .thenReturn(new CmiCheckoutSession("MOCK-1", "/api/v1/mock-cmi/MOCK-1"));

    CheckoutResponse response = service.initiateCheckout(HOMEOWNER_ID, BOOKING_ID);

    assertThat(response.paymentId()).isEqualTo(winnerRow.getId());
  }

  @Test
  void processWebhookRejectsAnInvalidSignature() {
    assertThatThrownBy(() -> service.processWebhook("{\"transactionId\":\"t1\"}", "bad-signature"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(paymentRepository, never()).findByCmiTransactionId(any());
  }

  @Test
  void processWebhookRejectsAMalformedPayload() {
    String body = "not json";
    String signature = cmiSignatureService.sign(body);

    assertThatThrownBy(() -> service.processWebhook(body, signature))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void processWebhookRejectsAnUnknownTransaction() {
    String body =
        "{\"transactionId\":\"unknown\",\"status\":\"SUCCEEDED\",\"amount\":100,\"currency\":\"MAD\"}";
    when(paymentRepository.findByCmiTransactionId("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.processWebhook(body, cmiSignatureService.sign(body)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void processWebhookIsIdempotentOnAnAlreadySucceededTransaction() {
    Payment payment = new Payment(BOOKING_ID, new BigDecimal("100.00"), "MAD");
    payment.assignCmiTransactionId("t1");
    payment.markSucceeded();
    when(paymentRepository.findByCmiTransactionId("t1")).thenReturn(Optional.of(payment));
    String body =
        "{\"transactionId\":\"t1\",\"status\":\"SUCCEEDED\",\"amount\":100.00,\"currency\":\"MAD\"}";

    service.processWebhook(body, cmiSignatureService.sign(body));

    verify(bookingService, never()).markPaid(any());
    verify(paymentRepository, never()).save(any());
  }

  @Test
  void processWebhookRejectsAnAmountMismatch() {
    Payment payment = new Payment(BOOKING_ID, new BigDecimal("100.00"), "MAD");
    payment.assignCmiTransactionId("t1");
    when(paymentRepository.findByCmiTransactionId("t1")).thenReturn(Optional.of(payment));
    String body =
        "{\"transactionId\":\"t1\",\"status\":\"SUCCEEDED\",\"amount\":999.00,\"currency\":\"MAD\"}";

    assertThatThrownBy(() -> service.processWebhook(body, cmiSignatureService.sign(body)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.BAD_REQUEST);
    verify(bookingService, never()).markPaid(any());
  }

  @Test
  void processWebhookMarksPaymentSucceededAndBookingPaid() {
    Payment payment = new Payment(BOOKING_ID, new BigDecimal("100.00"), "MAD");
    payment.assignCmiTransactionId("t1");
    when(paymentRepository.findByCmiTransactionId("t1")).thenReturn(Optional.of(payment));
    String body =
        "{\"transactionId\":\"t1\",\"status\":\"SUCCEEDED\",\"amount\":100.00,\"currency\":\"MAD\"}";

    service.processWebhook(body, cmiSignatureService.sign(body));

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    verify(bookingService).markPaid(BOOKING_ID);
    verify(notificationService).notifyPaymentSucceeded(BOOKING_ID, payment.getId());
  }

  @Test
  void processWebhookMarksPaymentFailedAndLeavesBookingUntouched() {
    Payment payment = new Payment(BOOKING_ID, new BigDecimal("100.00"), "MAD");
    payment.assignCmiTransactionId("t1");
    when(paymentRepository.findByCmiTransactionId("t1")).thenReturn(Optional.of(payment));
    String body =
        "{\"transactionId\":\"t1\",\"status\":\"FAILED\",\"amount\":100.00,\"currency\":\"MAD\"}";

    service.processWebhook(body, cmiSignatureService.sign(body));

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    verify(bookingService, never()).markPaid(any());
  }
}
