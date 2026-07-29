package com.shamsma.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.shamsma.api.TestcontainersConfiguration;
import com.shamsma.api.shared.User;
import com.shamsma.api.shared.UserRepository;
import com.shamsma.api.shared.UserRole;
import java.math.BigDecimal;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers Story 5.1 (admin booking/payment overview) end-to-end: a real booking+payment flow shows
 * up in the admin list, a review flag (seeded directly via SQL — the webhook mismatch path that
 * actually raises one is already covered by PaymentFlowIntegrationTest) can be resolved/dismissed
 * and is audited, non-admins are rejected, and CSV export produces the right rows.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.rate-limit.capacity=1000")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminBookingOverviewIntegrationTest {

  private static final String PASSWORD = "Xk9$vTqzR7wLpN";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private String homeownerToken;
  private String installerToken;
  private String adminToken;
  private UUID installerId;

  @BeforeAll
  void setUpActors() throws Exception {
    homeownerToken = registerLoginHomeowner();

    String installerEmail = "installer-admin-" + UUID.randomUUID() + "@example.com";
    installerToken = registerLoginInstaller(installerEmail, "Solaire Admin");
    installerId = installerIdFor(installerEmail);
    jdbcTemplate.update(
        "UPDATE installers SET verification_status = 'APPROVED' WHERE user_id = ?", installerId);

    adminToken = registerLoginMfaEnrolledAdmin();
  }

  @Test
  void adminSeesABookedAndPaidBookingWithPartyNames() throws Exception {
    String bookingId = bookAQuote(new BigDecimal("50000"));
    String transactionId = checkout(bookingId);
    mockMvc
        .perform(post("/api/v1/mock-cmi/" + transactionId + "/succeed"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/admin/bookings")
                .param("search", "Solaire Admin")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].installerBusinessName", is("Solaire Admin")))
        .andExpect(jsonPath("$.content[0].paymentStatus", is("SUCCEEDED")))
        .andExpect(jsonPath("$.content[0].bookingStatus", is("BOOKED")));
  }

  @Test
  void nonAdminCannotReachTheOverview() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/bookings").header("Authorization", "Bearer " + homeownerToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void needsReviewOnlyFilterAndResolveWorkflow() throws Exception {
    String bookingId = bookAQuote(new BigDecimal("10000"));
    String transactionId = checkout(bookingId);
    UUID paymentId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM payments WHERE cmi_transaction_id = ?", UUID.class, transactionId);
    UUID flagId =
        jdbcTemplate.queryForObject(
            "INSERT INTO payment_review_flags (payment_id, reason, expected_amount, actual_amount) "
                + "VALUES (?, 'AMOUNT_MISMATCH', 1000.00, 999.00) RETURNING id",
            UUID.class,
            paymentId);

    mockMvc
        .perform(
            get("/api/v1/admin/bookings")
                .param("needsReviewOnly", "true")
                .param("search", "no-such-installer-zzz")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements", is(0)));

    mockMvc
        .perform(
            get("/api/v1/admin/bookings")
                .param("needsReviewOnly", "true")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.paymentId == '" + paymentId + "')]").exists());

    mockMvc
        .perform(
            post("/api/v1/admin/payment-flags/" + flagId + "/resolve")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"verified manually with bank statement\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/admin/payment-flags/" + flagId + "/resolve")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isConflict());

    Integer auditRows =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM audit_log WHERE action = 'PAYMENT_FLAG_RESOLVED' AND entity_id = ?",
            Integer.class,
            flagId);
    assertThat(auditRows).isEqualTo(1);

    mockMvc
        .perform(
            get("/api/v1/admin/bookings")
                .param("needsReviewOnly", "true")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.paymentId == '" + paymentId + "')]").doesNotExist());
  }

  @Test
  void csvExportContainsHeaderAndRows() throws Exception {
    bookAQuote(new BigDecimal("7000"));

    String csv =
        mockMvc
            .perform(
                get("/api/v1/admin/bookings/export")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(csv).startsWith("bookingId,bookingStatus,depositAmount");
    assertThat(csv.lines().count()).isGreaterThan(1);
  }

  private String checkout(String bookingId) throws Exception {
    String checkoutJson =
        mockMvc
            .perform(
                post("/api/v1/homeowner/bookings/" + bookingId + "/checkout")
                    .header("Authorization", "Bearer " + homeownerToken))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(checkoutJson, "$.cmiTransactionId");
  }

  private String bookAQuote(BigDecimal quoteAmount) throws Exception {
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
    String requestId = JsonPath.read(createJson, "$[0].id");

    mockMvc
        .perform(
            post("/api/v1/installer/quote-requests/" + requestId + "/respond")
                .header("Authorization", "Bearer " + installerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    ("{\"action\":\"QUOTE\",\"quoteAmount\":%s}")
                        .formatted(quoteAmount.toPlainString())))
        .andExpect(status().isOk());

    String bookJson =
        mockMvc
            .perform(
                post("/api/v1/homeowner/quote-requests/" + requestId + "/book")
                    .header("Authorization", "Bearer " + homeownerToken))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(bookJson, "$.id");
  }

  private UUID installerIdFor(String email) {
    return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, email);
  }

  private String registerLoginHomeowner() throws Exception {
    String email = "homeowner-admin-" + UUID.randomUUID() + "@example.com";
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

  private String registerLoginMfaEnrolledAdmin() throws Exception {
    User admin =
        userRepository.save(
            new User(
                "admin-overview-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode(PASSWORD),
                UserRole.ADMIN));
    String firstToken = login(admin.getEmail());
    mockMvc
        .perform(post("/api/v1/auth/mfa/enroll").header("Authorization", "Bearer " + firstToken))
        .andExpect(status().isOk());
    return login(admin.getEmail());
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
