package com.shamsma.api.installer;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class InstallerServiceImpl implements InstallerService {

  private final InstallerRepository installerRepository;

  InstallerServiceImpl(InstallerRepository installerRepository) {
    this.installerRepository = installerRepository;
  }

  @Override
  public void createProfile(UUID userId, String businessName, String phone) {
    installerRepository.save(new Installer(userId, businessName, phone));
  }
}
