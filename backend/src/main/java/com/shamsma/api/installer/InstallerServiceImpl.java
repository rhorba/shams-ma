package com.shamsma.api.installer;

import com.shamsma.api.shared.geocoding.GeoPoint;
import com.shamsma.api.shared.geocoding.GeocodingException;
import com.shamsma.api.shared.geocoding.GeocodingService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class InstallerServiceImpl implements InstallerService {

  private final InstallerRepository installerRepository;
  private final GeocodingService geocodingService;

  InstallerServiceImpl(InstallerRepository installerRepository, GeocodingService geocodingService) {
    this.installerRepository = installerRepository;
    this.geocodingService = geocodingService;
  }

  @Override
  public void createProfile(UUID userId, String businessName, String phone) {
    installerRepository.save(new Installer(userId, businessName, phone));
  }

  @Override
  public CoverageZoneResponse setCoverageZone(
      UUID userId, String addressText, BigDecimal radiusKm) {
    GeoPoint point;
    try {
      point = geocodingService.geocode(addressText);
    } catch (GeocodingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
    int updated =
        installerRepository.updateCoverageZone(userId, point.lat(), point.lng(), radiusKm);
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Installer not found");
    }
    return new CoverageZoneResponse(point.lat(), point.lng(), radiusKm);
  }

  @Override
  public List<InstallerBrowseResult> browse(double lat, double lng) {
    return installerRepository.findWithinRadius(lat, lng).stream()
        .map(
            row ->
                new InstallerBrowseResult(
                    row.getUserId(), row.getBusinessName(), row.getPhone(), row.getDistanceKm()))
        .toList();
  }
}
