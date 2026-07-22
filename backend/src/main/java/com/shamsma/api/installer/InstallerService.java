package com.shamsma.api.installer;

import java.util.UUID;

public interface InstallerService {

  void createProfile(UUID userId, String businessName, String phone);
}
