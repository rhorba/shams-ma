package com.shamsma.api.homeowner;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

  @Override
  public HomeownerSummary getSummary(UUID userId) {
    Homeowner homeowner =
        homeownerRepository
            .findById(userId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Homeowner not found"));
    return new HomeownerSummary(
        homeowner.getUserId(), homeowner.getFullName(), homeowner.getPhone());
  }
}
