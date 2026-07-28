package com.shamsma.api.payment;

import com.shamsma.api.booking.BookingResponse;
import com.shamsma.api.booking.BookingService;
import com.shamsma.api.booking.BookingStatus;
import com.shamsma.api.notification.NotificationService;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
class PaymentServiceImpl implements PaymentService {

  private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
  private static final String CURRENCY = "MAD";

  private final PaymentRepository paymentRepository;
  private final BookingService bookingService;
  private final CmiCheckoutService cmiCheckoutService;
  private final CmiSignatureService cmiSignatureService;
  private final NotificationService notificationService;
  private final ObjectMapper objectMapper;

  PaymentServiceImpl(
      PaymentRepository paymentRepository,
      BookingService bookingService,
      CmiCheckoutService cmiCheckoutService,
      CmiSignatureService cmiSignatureService,
      NotificationService notificationService,
      ObjectMapper objectMapper) {
    this.paymentRepository = paymentRepository;
    this.bookingService = bookingService;
    this.cmiCheckoutService = cmiCheckoutService;
    this.cmiSignatureService = cmiSignatureService;
    this.notificationService = notificationService;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public CheckoutResponse initiateCheckout(UUID homeownerId, UUID bookingId) {
    BookingResponse booking = bookingService.getOwnedBooking(homeownerId, bookingId);
    if (booking.status() != BookingStatus.PENDING_PAYMENT) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Booking is not awaiting payment (current status: " + booking.status() + ")");
    }

    Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
    if (payment != null && payment.getStatus() == PaymentStatus.SUCCEEDED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "This booking has already been paid");
    }
    if (payment == null) {
      payment = createPaymentRow(bookingId, booking.depositAmount());
    } else {
      payment.resetForRetry();
    }

    CmiCheckoutSession session =
        cmiCheckoutService.createSession(
            payment.getId(), payment.getAmount(), payment.getCurrency());
    payment.assignCmiTransactionId(session.cmiTransactionId());
    paymentRepository.save(payment);

    return new CheckoutResponse(
        payment.getId(),
        session.checkoutUrl(),
        session.cmiTransactionId(),
        payment.getAmount(),
        payment.getCurrency());
  }

  @Override
  @Transactional
  public void processWebhook(String rawBody, String signature) {
    if (!cmiSignatureService.verify(rawBody, signature)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
    }

    CmiWebhookPayload payload;
    try {
      payload = objectMapper.readValue(rawBody, CmiWebhookPayload.class);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed webhook payload");
    }

    Payment payment =
        paymentRepository
            .findByCmiTransactionId(payload.transactionId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Unknown transaction: " + payload.transactionId()));

    if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
      // Idempotent replay of an already-processed success — no state change, no error either.
      return;
    }

    if (payload.amount().compareTo(payment.getAmount()) != 0) {
      // No "system" user exists to attribute an audit_log row to (actor_user_id is NOT NULL,
      // FK'd to users) — logging is the flag-for-review mechanism until Epic 5 (Admin Oversight)
      // adds a real review surface for this.
      log.warn(
          "Webhook amount mismatch for payment={} transactionId={}: expected={} got={}",
          payment.getId(),
          payload.transactionId(),
          payment.getAmount(),
          payload.amount());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount mismatch");
    }

    if ("SUCCEEDED".equals(payload.status())) {
      payment.markSucceeded();
      paymentRepository.save(payment);
      bookingService.markPaid(payment.getBookingId());
      notificationService.notifyPaymentSucceeded(payment.getBookingId(), payment.getId());
    } else {
      payment.markFailed();
      paymentRepository.save(payment);
    }
  }

  private Payment createPaymentRow(UUID bookingId, BigDecimal depositAmount) {
    try {
      return paymentRepository.save(new Payment(bookingId, depositAmount, CURRENCY));
    } catch (DataIntegrityViolationException e) {
      // Lost a race to a concurrent request for the same booking (payments.booking_id is
      // UNIQUE) — fetch the row the other request just created instead of failing.
      return paymentRepository.findByBookingId(bookingId).orElseThrow(() -> e);
    }
  }
}
