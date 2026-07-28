package com.shamsma.api.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shamsma.api.homeowner.HomeownerService;
import com.shamsma.api.homeowner.HomeownerSummary;
import com.shamsma.api.installer.InstallerService;
import com.shamsma.api.installer.InstallerSummary;
import com.shamsma.api.installer.VerificationStatus;
import com.shamsma.api.notification.NotificationService;
import java.math.BigDecimal;
import java.util.List;
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
class QuoteRequestServiceImplTest {

  @Mock private QuoteRequestRepository quoteRequestRepository;
  @Mock private BookingRepository bookingRepository;
  @Mock private InstallerService installerService;
  @Mock private HomeownerService homeownerService;
  @Mock private NotificationService notificationService;

  private QuoteRequestServiceImpl service;

  private final UUID homeownerId = UUID.randomUUID();
  private final UUID installerId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new QuoteRequestServiceImpl(
            quoteRequestRepository,
            bookingRepository,
            installerService,
            homeownerService,
            notificationService);
  }

  private InstallerSummary approvedInstaller() {
    return new InstallerSummary(
        installerId, "Solaire Atlas", "+212600000000", VerificationStatus.APPROVED);
  }

  private HomeownerSummary homeowner() {
    return new HomeownerSummary(homeownerId, "Test Homeowner", "0600000000");
  }

  @Test
  void requestQuotesCreatesOneRowPerInstallerAndNotifies() {
    when(homeownerService.getSummary(homeownerId)).thenReturn(homeowner());
    when(installerService.getSummary(installerId)).thenReturn(approvedInstaller());
    when(quoteRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    List<QuoteRequestSummary> created =
        service.requestQuotes(
            homeownerId,
            List.of(installerId),
            "hello",
            new BigDecimal("6500"),
            new BigDecimal("7.4"));

    assertThat(created).hasSize(1);
    assertThat(created.get(0).status()).isEqualTo(QuoteStatus.REQUESTED);
    assertThat(created.get(0).installerBusinessName()).isEqualTo("Solaire Atlas");
    verify(notificationService).notifyNewQuoteRequest(installerId, null);
  }

  @Test
  void requestQuotesRejectsUnapprovedInstaller() {
    when(homeownerService.getSummary(homeownerId)).thenReturn(homeowner());
    when(installerService.getSummary(installerId))
        .thenReturn(
            new InstallerSummary(installerId, "Solaire Atlas", null, VerificationStatus.PENDING));

    assertThatThrownBy(
            () -> service.requestQuotes(homeownerId, List.of(installerId), null, null, null))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.BAD_REQUEST);
    verify(quoteRequestRepository, never()).save(any());
  }

  @Test
  void respondQuotesSetsStatusAndNotifiesHomeowner() {
    UUID requestId = UUID.randomUUID();
    QuoteRequest request = new QuoteRequest(homeownerId, installerId, "hi", null, null);
    when(quoteRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
    when(installerService.getSummary(installerId)).thenReturn(approvedInstaller());
    when(homeownerService.getSummary(homeownerId)).thenReturn(homeowner());

    QuoteRequestSummary summary =
        service.respond(installerId, requestId, QuoteAction.QUOTE, new BigDecimal("5000"), "notes");

    assertThat(summary.status()).isEqualTo(QuoteStatus.QUOTED);
    assertThat(summary.quoteAmount()).isEqualByComparingTo("5000");
    verify(notificationService)
        .notifyQuoteResponse(homeownerId, request.getId(), true, new BigDecimal("5000"));
  }

  @Test
  void respondDeclineSetsStatusDeclined() {
    UUID requestId = UUID.randomUUID();
    QuoteRequest request = new QuoteRequest(homeownerId, installerId, null, null, null);
    when(quoteRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
    when(installerService.getSummary(installerId)).thenReturn(approvedInstaller());
    when(homeownerService.getSummary(homeownerId)).thenReturn(homeowner());

    QuoteRequestSummary summary =
        service.respond(installerId, requestId, QuoteAction.DECLINE, null, "not now");

    assertThat(summary.status()).isEqualTo(QuoteStatus.DECLINED);
  }

  @Test
  void respondRejectsMissingQuoteAmountWhenQuoting() {
    UUID requestId = UUID.randomUUID();
    QuoteRequest request = new QuoteRequest(homeownerId, installerId, null, null, null);
    when(quoteRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

    assertThatThrownBy(() -> service.respond(installerId, requestId, QuoteAction.QUOTE, null, null))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void respondRejectsNonOwningInstaller() {
    UUID requestId = UUID.randomUUID();
    QuoteRequest request = new QuoteRequest(homeownerId, installerId, null, null, null);
    when(quoteRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

    UUID otherInstallerId = UUID.randomUUID();
    assertThatThrownBy(
            () -> service.respond(otherInstallerId, requestId, QuoteAction.DECLINE, null, null))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void respondRejectsAlreadyRespondedRequest() {
    UUID requestId = UUID.randomUUID();
    QuoteRequest request = new QuoteRequest(homeownerId, installerId, null, null, null);
    request.markDeclined(null);
    when(quoteRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

    assertThatThrownBy(
            () -> service.respond(installerId, requestId, QuoteAction.QUOTE, BigDecimal.TEN, null))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void bookCreatesPendingPaymentBookingWithTenPercentDeposit() {
    UUID requestId = UUID.randomUUID();
    QuoteRequest request = new QuoteRequest(homeownerId, installerId, null, null, null);
    request.markQuoted(new BigDecimal("50000"), "notes");
    when(quoteRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
    when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    BookingResponse response = service.book(homeownerId, requestId);

    assertThat(response.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    assertThat(response.depositAmount()).isEqualByComparingTo("5000.00");
    assertThat(request.getStatus()).isEqualTo(QuoteStatus.BOOKED);
  }

  @Test
  void bookRejectsRequestThatIsNotYetQuoted() {
    UUID requestId = UUID.randomUUID();
    QuoteRequest request = new QuoteRequest(homeownerId, installerId, null, null, null);
    when(quoteRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

    assertThatThrownBy(() -> service.book(homeownerId, requestId))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.BAD_REQUEST);
    verify(bookingRepository, never()).save(any());
  }

  @Test
  void bookRejectsNonOwningHomeowner() {
    UUID requestId = UUID.randomUUID();
    QuoteRequest request = new QuoteRequest(homeownerId, installerId, null, null, null);
    request.markQuoted(BigDecimal.TEN, null);
    when(quoteRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

    assertThatThrownBy(() -> service.book(UUID.randomUUID(), requestId))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void bookRejectsUnknownRequest() {
    when(quoteRequestRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.book(homeownerId, UUID.randomUUID()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("not found");
  }
}
