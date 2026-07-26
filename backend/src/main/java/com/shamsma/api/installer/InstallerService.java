package com.shamsma.api.installer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InstallerService {

  void createProfile(UUID userId, String businessName, String phone);

  /**
   * Geocodes {@code addressText} and sets the installer's coverage-zone center/radius. Returns the
   * resolved point so the frontend's map picker can show the installer where their address geocoded
   * to (catches wrong-address geocoding results).
   */
  CoverageZoneResponse setCoverageZone(UUID userId, String addressText, BigDecimal radiusKm);

  /** Public browse: approved installers whose coverage zone contains the given point. */
  List<InstallerBrowseResult> browse(double lat, double lng);
}
