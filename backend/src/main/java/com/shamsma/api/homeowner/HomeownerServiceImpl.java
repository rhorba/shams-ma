package com.shamsma.api.homeowner;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class HomeownerServiceImpl implements HomeownerService {

  private final HomeownerRepository homeownerRepository;

  HomeownerServiceImpl(HomeownerRepository homeownerRepository) {
    this.homeownerRepository = homeownerRepository;
  }

  @Override
  public void createProfile(UUID userId, String fullName, String phone, String addressText) {
    homeownerRepository.save(new Homeowner(userId, fullName, phone, addressText));
  }
}
