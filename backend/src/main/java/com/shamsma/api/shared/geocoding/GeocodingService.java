package com.shamsma.api.shared.geocoding;

public interface GeocodingService {

  /**
   * Resolves a free-text address to coordinates.
   *
   * @throws GeocodingException if the address can't be resolved or the provider errors out
   */
  GeoPoint geocode(String addressText);
}
