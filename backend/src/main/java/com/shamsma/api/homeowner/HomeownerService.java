package com.shamsma.api.homeowner;

import java.util.UUID;

public interface HomeownerService {

  void createProfile(UUID userId, String fullName, String phone, String addressText);
}
