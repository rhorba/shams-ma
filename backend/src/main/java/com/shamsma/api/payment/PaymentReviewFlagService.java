package com.shamsma.api.payment;

import java.util.UUID;

public interface PaymentReviewFlagService {

  /** Only an OPEN flag can be resolved or dismissed. */
  void resolve(UUID flagId, UUID adminUserId, String note);

  void dismiss(UUID flagId, UUID adminUserId, String note);
}
