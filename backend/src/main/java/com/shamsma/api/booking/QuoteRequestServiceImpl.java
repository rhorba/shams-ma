package com.shamsma.api.booking;

import com.shamsma.api.homeowner.HomeownerService;
import com.shamsma.api.homeowner.HomeownerSummary;
import com.shamsma.api.installer.InstallerService;
import com.shamsma.api.installer.InstallerSummary;
import com.shamsma.api.installer.VerificationStatus;
import com.shamsma.api.notification.NotificationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class QuoteRequestServiceImpl implements QuoteRequestService {

  /**
   * No deposit-percentage rule is defined anywhere in the docs (PRD/database docs only say a
   * deposit is charged, not how much) — 10% of the installer's quote is a placeholder MVP default.
   * Revisit when Epic 4 (Payments) actually wires up CMI checkout amounts.
   */
  private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.10");

  private final QuoteRequestRepository quoteRequestRepository;
  private final BookingRepository bookingRepository;
  private final InstallerService installerService;
  private final HomeownerService homeownerService;
  private final NotificationService notificationService;

  QuoteRequestServiceImpl(
      QuoteRequestRepository quoteRequestRepository,
      BookingRepository bookingRepository,
      InstallerService installerService,
      HomeownerService homeownerService,
      NotificationService notificationService) {
    this.quoteRequestRepository = quoteRequestRepository;
    this.bookingRepository = bookingRepository;
    this.installerService = installerService;
    this.homeownerService = homeownerService;
    this.notificationService = notificationService;
  }

  @Override
  @Transactional
  public List<QuoteRequestSummary> requestQuotes(
      UUID homeownerId,
      List<UUID> installerIds,
      String message,
      BigDecimal roiEstimateKwh,
      BigDecimal roiPaybackYears) {
    HomeownerSummary homeowner = homeownerService.getSummary(homeownerId);
    List<QuoteRequestSummary> created =
        installerIds.stream()
            .map(
                installerId -> {
                  InstallerSummary installer = installerService.getSummary(installerId);
                  if (installer.verificationStatus() != VerificationStatus.APPROVED) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Installer " + installerId + " is not a verified installer");
                  }
                  QuoteRequest saved =
                      quoteRequestRepository.save(
                          new QuoteRequest(
                              homeownerId, installerId, message, roiEstimateKwh, roiPaybackYears));
                  notificationService.notifyNewQuoteRequest(installerId, saved.getId());
                  return toSummary(saved, installer, homeowner);
                })
            .toList();
    return created;
  }

  @Override
  public List<QuoteRequestSummary> listForHomeowner(UUID homeownerId) {
    HomeownerSummary homeowner = homeownerService.getSummary(homeownerId);
    return quoteRequestRepository.findByHomeownerIdOrderByCreatedAtDesc(homeownerId).stream()
        .map(qr -> toSummary(qr, installerService.getSummary(qr.getInstallerId()), homeowner))
        .toList();
  }

  @Override
  public List<QuoteRequestSummary> listForInstaller(UUID installerId) {
    InstallerSummary installer = installerService.getSummary(installerId);
    return quoteRequestRepository.findByInstallerIdOrderByCreatedAtDesc(installerId).stream()
        .map(qr -> toSummary(qr, installer, homeownerService.getSummary(qr.getHomeownerId())))
        .toList();
  }

  @Override
  @Transactional
  public QuoteRequestSummary respond(
      UUID installerId,
      UUID requestId,
      QuoteAction action,
      BigDecimal quoteAmount,
      String quoteNotes) {
    QuoteRequest request = findOwnedByInstaller(requestId, installerId);
    if (request.getStatus() != QuoteStatus.REQUESTED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This quote request has already been responded to");
    }

    boolean quoted = action == QuoteAction.QUOTE;
    if (quoted) {
      if (quoteAmount == null || quoteAmount.signum() <= 0) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "quoteAmount is required and must be positive to quote");
      }
      request.markQuoted(quoteAmount, quoteNotes);
    } else {
      request.markDeclined(quoteNotes);
    }
    quoteRequestRepository.save(request);

    notificationService.notifyQuoteResponse(
        request.getHomeownerId(), request.getId(), quoted, request.getQuoteAmount());

    return toSummary(
        request,
        installerService.getSummary(installerId),
        homeownerService.getSummary(request.getHomeownerId()));
  }

  @Override
  @Transactional
  public BookingResponse book(UUID homeownerId, UUID requestId) {
    QuoteRequest request = findOwnedByHomeowner(requestId, homeownerId);
    if (request.getStatus() != QuoteStatus.QUOTED) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Can only book a request that has been quoted (current status: "
              + request.getStatus()
              + ")");
    }

    BigDecimal depositAmount =
        request.getQuoteAmount().multiply(DEPOSIT_RATE).setScale(2, RoundingMode.HALF_UP);
    Booking booking = bookingRepository.save(new Booking(request.getId(), depositAmount));

    request.markBooked();
    quoteRequestRepository.save(request);

    return BookingResponse.from(booking);
  }

  private QuoteRequest findOwnedByInstaller(UUID requestId, UUID installerId) {
    QuoteRequest request = findOrThrow(requestId);
    if (!request.getInstallerId().equals(installerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your quote request");
    }
    return request;
  }

  private QuoteRequest findOwnedByHomeowner(UUID requestId, UUID homeownerId) {
    QuoteRequest request = findOrThrow(requestId);
    if (!request.getHomeownerId().equals(homeownerId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your quote request");
    }
    return request;
  }

  private QuoteRequest findOrThrow(UUID requestId) {
    return quoteRequestRepository
        .findById(requestId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote request not found"));
  }

  private static QuoteRequestSummary toSummary(
      QuoteRequest qr, InstallerSummary installer, HomeownerSummary homeowner) {
    return new QuoteRequestSummary(
        qr.getId(),
        installer.userId(),
        installer.businessName(),
        homeowner.userId(),
        homeowner.fullName(),
        qr.getStatus(),
        qr.getMessage(),
        qr.getRoiEstimateKwh(),
        qr.getRoiPaybackYears(),
        qr.getQuoteAmount(),
        qr.getQuoteNotes(),
        qr.getRespondedAt(),
        qr.getCreatedAt());
  }
}
