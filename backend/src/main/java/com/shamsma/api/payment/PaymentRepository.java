package com.shamsma.api.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentRepository extends JpaRepository<Payment, UUID> {

  Optional<Payment> findByBookingId(UUID bookingId);

  Optional<Payment> findByCmiTransactionId(String cmiTransactionId);

  List<Payment> findByBookingIdIn(List<UUID> bookingIds);
}
