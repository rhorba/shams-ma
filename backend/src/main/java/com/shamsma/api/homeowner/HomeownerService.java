package com.shamsma.api.homeowner;

import java.util.UUID;

public interface HomeownerService {

  void createProfile(UUID userId, String fullName, String phone, String addressText);

  /** Cross-package lookup (e.g. for booking) — throws 404 if the homeowner doesn't exist. */
  HomeownerSummary getSummary(UUID userId);
}
