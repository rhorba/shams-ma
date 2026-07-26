package com.shamsma.api.shared.geocoding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

/**
 * Geocodes via Nominatim (OpenStreetMap) — free, no API key. Usage policy requires a descriptive
 * User-Agent identifying the application (see
 * https://operations.osmfoundation.org/policies/nominatim/).
 */
@Service
class NominatimGeocodingService implements GeocodingService {

  private final RestClient restClient;
  private final String baseUrl;

  @Autowired
  NominatimGeocodingService(
      @Value("${geocoding.base-url}") String baseUrl,
      @Value("${geocoding.user-agent}") String userAgent) {
    this(RestClient.builder(), baseUrl, userAgent);
  }

  /**
   * Package-visible for tests to bind a {@link
   * org.springframework.test.web.client.MockRestServiceServer}.
   */
  NominatimGeocodingService(
      RestClient.Builder restClientBuilder, String baseUrl, String userAgent) {
    this.baseUrl = baseUrl;
    this.restClient =
        restClientBuilder
            .defaultHeader("User-Agent", userAgent)
            .defaultHeader("Accept", "application/json")
            .build();
  }

  @Override
  public GeoPoint geocode(String addressText) {
    String uri =
        UriComponentsBuilder.fromUriString(baseUrl)
            .path("/search")
            .queryParam("q", addressText)
            .queryParam("format", "json")
            .queryParam("limit", 1)
            .build()
            .toUriString();

    JsonNode results;
    try {
      results = restClient.get().uri(uri).retrieve().body(JsonNode.class);
    } catch (Exception e) {
      throw new GeocodingException("Geocoding provider request failed", e);
    }

    if (results == null || !results.isArray() || results.isEmpty()) {
      throw new GeocodingException("Could not resolve address: " + addressText);
    }

    JsonNode first = results.get(0);
    return parse(first, addressText);
  }

  private static GeoPoint parse(JsonNode node, String addressText) {
    try {
      double lat = Double.parseDouble(node.get("lat").asText());
      double lng = Double.parseDouble(node.get("lon").asText());
      return new GeoPoint(lat, lng);
    } catch (RuntimeException e) {
      throw new GeocodingException("Malformed geocoding response for address: " + addressText, e);
    }
  }
}
