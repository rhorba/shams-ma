package com.shamsma.api.roi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shamsma.api.TestcontainersConfiguration;
import com.shamsma.api.shared.geocoding.GeoPoint;
import com.shamsma.api.shared.geocoding.GeocodingException;
import com.shamsma.api.shared.geocoding.GeocodingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers Story 2.1 end-to-end. GeocodingService is mocked — same "no live network dependency in CI"
 * reasoning as InstallerCoverageZoneIntegrationTest. A real Postgres/PostGIS Testcontainer is still
 * needed only because it backs the whole Spring context (this endpoint itself touches no DB).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.rate-limit.capacity=1000")
@AutoConfigureMockMvc
class RoiEstimateIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private GeocodingService geocodingService;

  @Test
  void estimateIsPublicNoAuthRequired() throws Exception {
    Mockito.when(geocodingService.geocode("Rabat, Morocco"))
        .thenReturn(new GeoPoint(34.0209, -6.8416));

    mockMvc
        .perform(
            get("/api/v1/roi/estimate")
                .param("address", "Rabat, Morocco")
                .param("monthlyBillMad", "500"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resolvedCity", org.hamcrest.Matchers.is("Rabat")))
        .andExpect(jsonPath("$.paybackYears").isNotEmpty());
  }

  @Test
  void rejectsNonPositiveMonthlyBill() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/roi/estimate")
                .param("address", "Rabat, Morocco")
                .param("monthlyBillMad", "0"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsBlankAddress() throws Exception {
    mockMvc
        .perform(get("/api/v1/roi/estimate").param("address", " ").param("monthlyBillMad", "500"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void translatesUnresolvableAddressTo400() throws Exception {
    Mockito.when(geocodingService.geocode("nowhere, nonsense"))
        .thenThrow(new GeocodingException("Could not resolve address"));

    mockMvc
        .perform(
            get("/api/v1/roi/estimate")
                .param("address", "nowhere, nonsense")
                .param("monthlyBillMad", "500"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void roofSizeCapsTheSystemWhenSmallerThanDemandSizing() throws Exception {
    Mockito.when(geocodingService.geocode("Agadir, Morocco"))
        .thenReturn(new GeoPoint(30.4278, -9.5981));

    // A 3000 MAD/month bill demand-sizes to ~17.7 kWp; a 30m² roof caps it at 30/6 = 5.0 kWp.
    mockMvc
        .perform(
            get("/api/v1/roi/estimate")
                .param("address", "Agadir, Morocco")
                .param("monthlyBillMad", "3000")
                .param("roofSizeM2", "30")
                .param("orientation", "SOUTH"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estimatedSystemKwp", org.hamcrest.Matchers.is(5.0)));
  }
}
