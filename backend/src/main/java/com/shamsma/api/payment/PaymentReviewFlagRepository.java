package com.shamsma.api.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentReviewFlagRepository extends JpaRepository<PaymentReviewFlag, UUID> {

  List<PaymentReviewFlag> findByPaymentIdInAndStatus(
      List<UUID> paymentIds, PaymentReviewFlagStatus status);

  Optional<PaymentReviewFlag> findByPaymentIdAndStatus(
      UUID paymentId, PaymentReviewFlagStatus status);
}
