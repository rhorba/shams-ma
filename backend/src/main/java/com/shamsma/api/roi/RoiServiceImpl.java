package com.shamsma.api.roi;

import com.shamsma.api.shared.geocoding.GeoPoint;
import com.shamsma.api.shared.geocoding.GeocodingException;
import com.shamsma.api.shared.geocoding.GeocodingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class RoiServiceImpl implements RoiService {

  private final GeocodingService geocodingService;

  RoiServiceImpl(GeocodingService geocodingService) {
    this.geocodingService = geocodingService;
  }

  @Override
  public RoiEstimateResponse estimate(
      String address, double monthlyBillMad, Double roofSizeM2, RoiOrientation orientation) {
    GeoPoint point;
    try {
      point = geocodingService.geocode(address);
    } catch (GeocodingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
    return RoiCalculator.estimate(
        point.lat(), point.lng(), monthlyBillMad, roofSizeM2, orientation);
  }
}
