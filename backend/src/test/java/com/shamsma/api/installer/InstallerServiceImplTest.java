package com.shamsma.api.installer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shamsma.api.shared.geocoding.GeoPoint;
import com.shamsma.api.shared.geocoding.GeocodingException;
import com.shamsma.api.shared.geocoding.GeocodingService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class InstallerServiceImplTest {

  @Mock private InstallerRepository installerRepository;
  @Mock private GeocodingService geocodingService;

  private InstallerServiceImpl service;

  @Test
  void setCoverageZoneGeocodesThenPersists() {
    service = new InstallerServiceImpl(installerRepository, geocodingService);
    UUID userId = UUID.randomUUID();
    when(geocodingService.geocode("Rabat, Morocco")).thenReturn(new GeoPoint(34.02, -6.83));
    when(installerRepository.updateCoverageZone(any(), anyDouble(), anyDouble(), any()))
        .thenReturn(1);

    CoverageZoneResponse response =
        service.setCoverageZone(userId, "Rabat, Morocco", new BigDecimal("25"));

    verify(installerRepository).updateCoverageZone(userId, 34.02, -6.83, new BigDecimal("25"));
    assertThat(response).isEqualTo(new CoverageZoneResponse(34.02, -6.83, new BigDecimal("25")));
  }

  @Test
  void setCoverageZoneTranslatesGeocodingFailureTo400() {
    service = new InstallerServiceImpl(installerRepository, geocodingService);
    when(geocodingService.geocode("nonsense")).thenThrow(new GeocodingException("bad address"));

    assertThatThrownBy(() -> service.setCoverageZone(UUID.randomUUID(), "nonsense", BigDecimal.TEN))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("bad address");
  }

  @Test
  void setCoverageZoneRejectsUnknownInstaller() {
    service = new InstallerServiceImpl(installerRepository, geocodingService);
    when(geocodingService.geocode(any())).thenReturn(new GeoPoint(0, 0));
    when(installerRepository.updateCoverageZone(any(), anyDouble(), anyDouble(), any()))
        .thenReturn(0);

    assertThatThrownBy(
            () -> service.setCoverageZone(UUID.randomUUID(), "somewhere", BigDecimal.ONE))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void browseMapsRepositoryRowsToResults() {
    service = new InstallerServiceImpl(installerRepository, geocodingService);
    UUID installerId = UUID.randomUUID();
    InstallerBrowseRow row =
        new InstallerBrowseRow() {
          @Override
          public UUID getUserId() {
            return installerId;
          }

          @Override
          public String getBusinessName() {
            return "Solaire Atlas";
          }

          @Override
          public String getPhone() {
            return "+212600000000";
          }

          @Override
          public Double getDistanceKm() {
            return 4.2;
          }
        };
    when(installerRepository.findWithinRadius(34.0, -6.8)).thenReturn(List.of(row));

    List<InstallerBrowseResult> results = service.browse(34.0, -6.8);

    assertThat(results)
        .containsExactly(
            new InstallerBrowseResult(installerId, "Solaire Atlas", "+212600000000", 4.2));
  }

  @Test
  void browseByAddressGeocodesThenDelegatesToBrowse() {
    service = new InstallerServiceImpl(installerRepository, geocodingService);
    when(geocodingService.geocode("Rabat, Morocco")).thenReturn(new GeoPoint(34.02, -6.84));
    when(installerRepository.findWithinRadius(34.02, -6.84)).thenReturn(List.of());

    List<InstallerBrowseResult> results = service.browseByAddress("Rabat, Morocco");

    assertThat(results).isEmpty();
    verify(installerRepository).findWithinRadius(34.02, -6.84);
  }

  @Test
  void browseByAddressTranslatesGeocodingFailureTo400() {
    service = new InstallerServiceImpl(installerRepository, geocodingService);
    when(geocodingService.geocode("nonsense")).thenThrow(new GeocodingException("bad address"));

    assertThatThrownBy(() -> service.browseByAddress("nonsense"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("bad address");
  }
}
