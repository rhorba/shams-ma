package com.shamsma.api.payment;

import com.shamsma.api.shared.AuditLogService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class PaymentReviewFlagServiceImpl implements PaymentReviewFlagService {

  private final PaymentReviewFlagRepository paymentReviewFlagRepository;
  private final AuditLogService auditLogService;

  PaymentReviewFlagServiceImpl(
      PaymentReviewFlagRepository paymentReviewFlagRepository, AuditLogService auditLogService) {
    this.paymentReviewFlagRepository = paymentReviewFlagRepository;
    this.auditLogService = auditLogService;
  }

  @Override
  @Transactional
  public void resolve(UUID flagId, UUID adminUserId, String note) {
    transition(
        flagId, adminUserId, note, PaymentReviewFlagStatus.RESOLVED, "PAYMENT_FLAG_RESOLVED");
  }

  @Override
  @Transactional
  public void dismiss(UUID flagId, UUID adminUserId, String note) {
    transition(
        flagId, adminUserId, note, PaymentReviewFlagStatus.DISMISSED, "PAYMENT_FLAG_DISMISSED");
  }

  private void transition(
      UUID flagId,
      UUID adminUserId,
      String note,
      PaymentReviewFlagStatus newStatus,
      String action) {
    PaymentReviewFlag flag =
        paymentReviewFlagRepository
            .findById(flagId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review flag not found"));
    if (flag.getStatus() != PaymentReviewFlagStatus.OPEN) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This flag has already been reviewed (" + flag.getStatus() + ")");
    }
    String previousStatus = flag.getStatus().name();
    flag.resolve(adminUserId, note, newStatus);
    paymentReviewFlagRepository.save(flag);
    auditLogService.record(
        adminUserId, action, "PAYMENT_REVIEW_FLAG", flagId, previousStatus, newStatus.name());
  }
}
