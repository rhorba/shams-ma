package com.shamsma.api.shared.geocoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NominatimGeocodingServiceTest {

  private static final String BASE_URL = "https://nominatim.example";
  private static final String USER_AGENT = "Shams.ma/1.0 (contact@shams.ma)";

  @Test
  void geocodesAddressToCoordinates() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(
            requestTo("https://nominatim.example/search?q=Rabat,%20Morocco&format=json&limit=1"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("User-Agent", USER_AGENT))
        .andRespond(
            withSuccess(
                "[{\"lat\":\"34.020882\",\"lon\":\"-6.841650\"}]", MediaType.APPLICATION_JSON));

    NominatimGeocodingService service =
        new NominatimGeocodingService(builder, BASE_URL, USER_AGENT);

    GeoPoint point = service.geocode("Rabat, Morocco");

    assertThat(point.lat()).isEqualTo(34.020882);
    assertThat(point.lng()).isEqualTo(-6.841650);
    server.verify();
  }

  @Test
  void throwsWhenNoResultsFound() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("https://nominatim.example/search?q=Nowhere&format=json&limit=1"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    NominatimGeocodingService service =
        new NominatimGeocodingService(builder, BASE_URL, USER_AGENT);

    assertThatThrownBy(() -> service.geocode("Nowhere")).isInstanceOf(GeocodingException.class);
  }
}
