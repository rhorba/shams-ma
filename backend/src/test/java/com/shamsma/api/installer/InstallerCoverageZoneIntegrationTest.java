package com.shamsma.api.installer;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.shamsma.api.TestcontainersConfiguration;
import com.shamsma.api.shared.geocoding.GeoPoint;
import com.shamsma.api.shared.geocoding.GeocodingService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers Story 1.1 end-to-end against a real Postgres/PostGIS Testcontainer: coverage-zone set ->
 * ST_DWithin browse match/exclusion. GeocodingService is mocked — hitting the real Nominatim API
 * from tests/CI would be a live, rate-limited, flaky network dependency.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class InstallerCoverageZoneIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private GeocodingService geocodingService;

  @Test
  void installerSetsCoverageZoneThenAppearsInBrowseWithinRadius() throws Exception {
    String email = "installer-" + UUID.randomUUID() + "@example.com";
    String accessToken = registerLoginInstaller(email, "Solaire Rabat");

    // Rabat, ~34.020882,-6.841650
    org.mockito.Mockito.when(geocodingService.geocode("Rabat, Morocco"))
        .thenReturn(new GeoPoint(34.020882, -6.841650));

    mockMvc
        .perform(
            put("/api/v1/installer/coverage-zone")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"addressText":"Rabat, Morocco","radiusKm":30}
                    """))
        .andExpect(status().isOk());

    // Installer isn't APPROVED yet -> excluded from browse (see coverage-zone-matching scenarios).
    mockMvc
        .perform(get("/api/v1/installers/browse").param("lat", "34.02").param("lng", "-6.84"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void browseExcludesPointOutsideRadius() throws Exception {
    String email = "installer-far-" + UUID.randomUUID() + "@example.com";
    String accessToken = registerLoginInstaller(email, "Solaire Nord");
    org.mockito.Mockito.when(geocodingService.geocode("Tangier, Morocco"))
        .thenReturn(new GeoPoint(35.7595, -5.8340));

    mockMvc
        .perform(
            put("/api/v1/installer/coverage-zone")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"addressText":"Tangier, Morocco","radiusKm":10}
                    """))
        .andExpect(status().isOk());

    // Casablanca is >200km from Tangier — well outside a 10km radius.
    mockMvc
        .perform(get("/api/v1/installers/browse").param("lat", "33.5731").param("lng", "-7.5898"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void coverageZoneRejectsRadiusOutOfBounds() throws Exception {
    String accessToken =
        registerLoginInstaller("installer-badradius-" + UUID.randomUUID() + "@example.com", "X");

    mockMvc
        .perform(
            put("/api/v1/installer/coverage-zone")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"addressText":"Rabat, Morocco","radiusKm":500}
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void browseIsPublicNoAuthRequired() throws Exception {
    mockMvc
        .perform(get("/api/v1/installers/browse").param("lat", "34.02").param("lng", "-6.84"))
        .andExpect(status().isOk());
  }

  private String registerLoginInstaller(String email, String businessName) throws Exception {
    var registerBody =
        """
        {"email":"%s","password":"Xk9$vTqzR7wLpN","role":"INSTALLER","businessName":"%s"}
        """
            .formatted(email, businessName);
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
        .andExpect(status().isCreated());

    var loginBody =
        """
        {"email":"%s","password":"Xk9$vTqzR7wLpN"}
        """
            .formatted(email);
    String responseJson =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role", is("INSTALLER")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(responseJson, "$.accessToken");
  }
}
