package com.shamsma.api.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shamsma.api.shared.AuditLogService;
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
class PaymentReviewFlagServiceImplTest {

  @Mock private PaymentReviewFlagRepository paymentReviewFlagRepository;
  @Mock private AuditLogService auditLogService;

  private PaymentReviewFlagServiceImpl service;

  private final UUID adminUserId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service = new PaymentReviewFlagServiceImpl(paymentReviewFlagRepository, auditLogService);
  }

  @Test
  void resolveFlipsAnOpenFlagToResolvedAndRecordsAnAuditEntry() {
    PaymentReviewFlag flag =
        new PaymentReviewFlag(
            UUID.randomUUID(),
            "AMOUNT_MISMATCH",
            new BigDecimal("100.00"),
            new BigDecimal("99.00"));
    when(paymentReviewFlagRepository.findById(flag.getId())).thenReturn(Optional.of(flag));

    service.resolve(flag.getId(), adminUserId, "checked manually, ok");

    assertThat(flag.getStatus()).isEqualTo(PaymentReviewFlagStatus.RESOLVED);
    assertThat(flag.getResolvedByUserId()).isEqualTo(adminUserId);
    verify(auditLogService)
        .record(
            adminUserId,
            "PAYMENT_FLAG_RESOLVED",
            "PAYMENT_REVIEW_FLAG",
            flag.getId(),
            "OPEN",
            "RESOLVED");
  }

  @Test
  void dismissFlipsAnOpenFlagToDismissed() {
    PaymentReviewFlag flag =
        new PaymentReviewFlag(
            UUID.randomUUID(),
            "AMOUNT_MISMATCH",
            new BigDecimal("100.00"),
            new BigDecimal("99.00"));
    when(paymentReviewFlagRepository.findById(flag.getId())).thenReturn(Optional.of(flag));

    service.dismiss(flag.getId(), adminUserId, null);

    assertThat(flag.getStatus()).isEqualTo(PaymentReviewFlagStatus.DISMISSED);
  }

  @Test
  void resolveRejectsAnAlreadyReviewedFlag() {
    PaymentReviewFlag flag =
        new PaymentReviewFlag(
            UUID.randomUUID(),
            "AMOUNT_MISMATCH",
            new BigDecimal("100.00"),
            new BigDecimal("99.00"));
    flag.resolve(UUID.randomUUID(), "already handled", PaymentReviewFlagStatus.DISMISSED);
    when(paymentReviewFlagRepository.findById(flag.getId())).thenReturn(Optional.of(flag));

    assertThatThrownBy(() -> service.resolve(flag.getId(), adminUserId, "note"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.CONFLICT);
    verify(auditLogService, never()).record(any(), any(), any(), any(), any(), any());
  }

  @Test
  void resolveRejectsAnUnknownFlag() {
    when(paymentReviewFlagRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.resolve(UUID.randomUUID(), adminUserId, null))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.NOT_FOUND);
  }
}
