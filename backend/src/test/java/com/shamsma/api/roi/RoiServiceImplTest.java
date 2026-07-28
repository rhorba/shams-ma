package com.shamsma.api.roi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.shamsma.api.shared.geocoding.GeoPoint;
import com.shamsma.api.shared.geocoding.GeocodingException;
import com.shamsma.api.shared.geocoding.GeocodingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RoiServiceImplTest {

  @Mock private GeocodingService geocodingService;

  @Test
  void geocodesAddressThenDelegatesToCalculator() {
    RoiServiceImpl service = new RoiServiceImpl(geocodingService);
    when(geocodingService.geocode("Rabat, Morocco")).thenReturn(new GeoPoint(34.0209, -6.8416));

    RoiEstimateResponse result =
        service.estimate("Rabat, Morocco", 500, null, RoiOrientation.SOUTH);

    assertThat(result.resolvedCity()).isEqualTo("Rabat");
    assertThat(result.lat()).isEqualTo(34.0209);
    assertThat(result.lng()).isEqualTo(-6.8416);
  }

  @Test
  void translatesGeocodingFailureTo400() {
    RoiServiceImpl service = new RoiServiceImpl(geocodingService);
    when(geocodingService.geocode("nonsense")).thenThrow(new GeocodingException("bad address"));

    assertThatThrownBy(() -> service.estimate("nonsense", 500, null, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("bad address");
  }
}
