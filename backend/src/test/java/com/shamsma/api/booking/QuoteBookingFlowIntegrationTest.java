package com.shamsma.api.booking;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.shamsma.api.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers Stories 3.1-3.3 end-to-end against a real Postgres Testcontainer: request -> lead inbox ->
 * respond -> book, plus the Test Strategy's IDOR/invalid-state-transition adversarial cases.
 * Installer approval is done via direct SQL (not the real cert-upload+admin-approve HTTP flow
 * already covered by CertificationFlowIntegrationTest) to keep this test focused on the
 * quote/booking workflow itself, same "don't re-test an unrelated component" rationale as
 * InstallerCoverageZoneIntegrationTest mocking GeocodingService.
 *
 * <p>Actors are registered once for the whole class (PER_CLASS lifecycle) rather than per test —
 * each test method creates fresh {@code QuoteRequest} rows (unrated) against shared accounts, so
 * this class's total registration calls stay well under RateLimitFilter's 10/min budget for {@code
 * /api/v1/auth/register} (a per-test-method registration count of ~15-20 across 7 tests previously
 * tripped 429s — a rate-limiter-vs-test-volume issue, not an app bug).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QuoteBookingFlowIntegrationTest {

  private static final String PASSWORD = "Xk9$vTqzR7wLpN";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  private String homeownerToken;
  private String otherHomeownerToken;
  private String installerAToken;
  private UUID installerAId;
  private String installerBToken;
  private UUID unapprovedInstallerId;

  @BeforeAll
  void setUpActors() throws Exception {
    homeownerToken = registerLoginHomeowner();
    otherHomeownerToken = registerLoginHomeowner();

    String emailA = "installer-a-" + UUID.randomUUID() + "@example.com";
    installerAToken = registerLoginInstaller(emailA, "Solaire A");
    installerAId = approveInstallerAndGetId(emailA);

    String emailB = "installer-b-" + UUID.randomUUID() + "@example.com";
    installerBToken = registerLoginInstaller(emailB, "Solaire B");
    approveInstallerAndGetId(emailB);

    String emailU = "installer-u-" + UUID.randomUUID() + "@example.com";
    registerLoginInstaller(emailU, "Solaire Unapproved");
    unapprovedInstallerId = installerIdFor(emailU);
  }

  @Test
  void fullFlow_requestThenQuoteThenBook() throws Exception {
    String createJson =
        mockMvc
            .perform(
                post("/api/v1/homeowner/quote-requests")
                    .header("Authorization", "Bearer " + homeownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"installerIds":["%s"],"message":"Interested in a 5kWp system","roiEstimateKwh":6500,"roiPaybackYears":7.4}
                        """
                            .formatted(installerAId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$[0].status", is("REQUESTED")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String requestId = JsonPath.read(createJson, "$[0].id");

    mockMvc
        .perform(
            get("/api/v1/installer/quote-requests")
                .header("Authorization", "Bearer " + installerAToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == '" + requestId + "')]", hasSize(1)));

    mockMvc
        .perform(
            post("/api/v1/installer/quote-requests/" + requestId + "/respond")
                .header("Authorization", "Bearer " + installerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"action":"QUOTE","quoteAmount":50000,"quoteNotes":"5kWp system, 10-year warranty"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("QUOTED")));

    mockMvc
        .perform(
            get("/api/v1/homeowner/quote-requests")
                .header("Authorization", "Bearer " + homeownerToken))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[?(@.id == '" + requestId + "')].status", is(java.util.List.of("QUOTED"))));

    mockMvc
        .perform(
            post("/api/v1/homeowner/quote-requests/" + requestId + "/book")
                .header("Authorization", "Bearer " + homeownerToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")))
        .andExpect(jsonPath("$.depositAmount", is(5000.0)));
  }

  @Test
  void installerDeclinesInsteadOfQuoting() throws Exception {
    String requestId = requestQuote(homeownerToken, installerAId);

    mockMvc
        .perform(
            post("/api/v1/installer/quote-requests/" + requestId + "/respond")
                .header("Authorization", "Bearer " + installerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"action":"DECLINE","quoteNotes":"Outside our service capacity right now"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("DECLINED")));
  }

  @Test
  void respondingTwiceIsRejected() throws Exception {
    String requestId = requestQuote(homeownerToken, installerAId);

    mockMvc
        .perform(
            post("/api/v1/installer/quote-requests/" + requestId + "/respond")
                .header("Authorization", "Bearer " + installerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"action":"DECLINE"}
                    """))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/installer/quote-requests/" + requestId + "/respond")
                .header("Authorization", "Bearer " + installerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"action":"QUOTE","quoteAmount":1000}
                    """))
        .andExpect(status().isConflict());
  }

  @Test
  void anotherInstallerCannotRespondToSomeoneElsesRequest() throws Exception {
    String requestId = requestQuote(homeownerToken, installerAId);

    mockMvc
        .perform(
            post("/api/v1/installer/quote-requests/" + requestId + "/respond")
                .header("Authorization", "Bearer " + installerBToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"action":"DECLINE"}
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  void bookingAStillRequestedRequestIsRejected() throws Exception {
    String requestId = requestQuote(homeownerToken, installerAId);

    mockMvc
        .perform(
            post("/api/v1/homeowner/quote-requests/" + requestId + "/book")
                .header("Authorization", "Bearer " + homeownerToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void anotherHomeownerCannotBookSomeoneElsesRequest() throws Exception {
    String requestId = requestQuote(homeownerToken, installerAId);
    mockMvc
        .perform(
            post("/api/v1/installer/quote-requests/" + requestId + "/respond")
                .header("Authorization", "Bearer " + installerAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"action":"QUOTE","quoteAmount":1000}
                    """))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/homeowner/quote-requests/" + requestId + "/book")
                .header("Authorization", "Bearer " + otherHomeownerToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void requestingAQuoteFromAnUnapprovedInstallerIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/homeowner/quote-requests")
                .header("Authorization", "Bearer " + homeownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"installerIds":["%s"]}
                    """
                        .formatted(unapprovedInstallerId)))
        .andExpect(status().isBadRequest());
  }

  private String requestQuote(String homeownerToken, UUID installerId) throws Exception {
    String createJson =
        mockMvc
            .perform(
                post("/api/v1/homeowner/quote-requests")
                    .header("Authorization", "Bearer " + homeownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"installerIds":["%s"]}
                        """
                            .formatted(installerId)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(createJson, "$[0].id");
  }

  private UUID approveInstallerAndGetId(String email) {
    UUID id = installerIdFor(email);
    jdbcTemplate.update(
        "UPDATE installers SET verification_status = 'APPROVED' WHERE user_id = ?", id);
    return id;
  }

  private UUID installerIdFor(String email) {
    return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, email);
  }

  private String registerLoginHomeowner() throws Exception {
    String email = "homeowner-" + UUID.randomUUID() + "@example.com";
    var registerBody =
        """
        {"email":"%s","password":"%s","role":"HOMEOWNER","fullName":"Test Homeowner","phone":"0600000000","addressText":"Rabat, Morocco"}
        """
            .formatted(email, PASSWORD);
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
        .andExpect(status().isCreated());
    return login(email);
  }

  private String registerLoginInstaller(String email, String businessName) throws Exception {
    var registerBody =
        """
        {"email":"%s","password":"%s","role":"INSTALLER","businessName":"%s"}
        """
            .formatted(email, PASSWORD, businessName);
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
        .andExpect(status().isCreated());
    return login(email);
  }

  private String login(String email) throws Exception {
    var loginBody =
        """
        {"email":"%s","password":"%s"}
        """
            .formatted(email, PASSWORD);
    String responseJson =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(responseJson, "$.accessToken");
  }
}
