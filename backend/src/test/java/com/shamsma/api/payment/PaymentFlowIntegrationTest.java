package com.shamsma.api.payment;

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
 * Covers Stories 4.1-4.2 end-to-end against a real Postgres Testcontainer: checkout -> mock CMI
 * trigger -> real signature-verified/idempotent webhook processing -> booking state machine, plus
 * the Test Strategy's payment adversarial checklist. Actors are shared across test methods
 * (PER_CLASS + @BeforeAll, same pattern as QuoteBookingFlowIntegrationTest) to stay under
 * RateLimitFilter's real 10/min budgets for /api/v1/auth/register and /api/v1/payments/webhook.
 *
 * <p>Direct-webhook tests sign payloads with the same dev-default secret application.yml falls back
 * to when CMI_SECRET is unset ("mock-cmi-dev-secret") — CmiSignatureService is package- private,
 * reachable here because this test lives in the same package.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.rate-limit.capacity=1000")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentFlowIntegrationTest {

  private static final String PASSWORD = "Xk9$vTqzR7wLpN";
  private static final CmiSignatureService SIGNER = new CmiSignatureService("mock-cmi-dev-secret");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  private String homeownerToken;
  private String otherHomeownerToken;
  private String installerToken;
  private UUID installerId;

  @BeforeAll
  void setUpActors() throws Exception {
    homeownerToken = registerLoginHomeowner();
    otherHomeownerToken = registerLoginHomeowner();

    String installerEmail = "installer-pay-" + UUID.randomUUID() + "@example.com";
    installerToken = registerLoginInstaller(installerEmail, "Solaire Pay");
    installerId = installerIdFor(installerEmail);
    jdbcTemplate.update(
        "UPDATE installers SET verification_status = 'APPROVED' WHERE user_id = ?", installerId);
  }

  @Test
  void fullFlow_checkoutThenMockSucceedConfirmsBooking() throws Exception {
    String bookingId = bookAQuote(new java.math.BigDecimal("50000"));

    String checkoutJson =
        mockMvc
            .perform(
                post("/api/v1/homeowner/bookings/" + bookingId + "/checkout")
                    .header("Authorization", "Bearer " + homeownerToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount", is(5000.0)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String transactionId = JsonPath.read(checkoutJson, "$.cmiTransactionId");

    mockMvc
        .perform(post("/api/v1/mock-cmi/" + transactionId + "/succeed"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/homeowner/bookings/" + bookingId)
                .header("Authorization", "Bearer " + homeownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("BOOKED")));
  }

  @Test
  void failedPaymentLeavesBookingPendingThenRetrySucceeds() throws Exception {
    String bookingId = bookAQuote(new java.math.BigDecimal("20000"));

    String firstCheckoutJson =
        mockMvc
            .perform(
                post("/api/v1/homeowner/bookings/" + bookingId + "/checkout")
                    .header("Authorization", "Bearer " + homeownerToken))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String firstTransactionId = JsonPath.read(firstCheckoutJson, "$.cmiTransactionId");

    mockMvc
        .perform(post("/api/v1/mock-cmi/" + firstTransactionId + "/fail"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/homeowner/bookings/" + bookingId)
                .header("Authorization", "Bearer " + homeownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")));

    String retryCheckoutJson =
        mockMvc
            .perform(
                post("/api/v1/homeowner/bookings/" + bookingId + "/checkout")
                    .header("Authorization", "Bearer " + homeownerToken))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String retryTransactionId = JsonPath.read(retryCheckoutJson, "$.cmiTransactionId");

    mockMvc
        .perform(post("/api/v1/mock-cmi/" + retryTransactionId + "/succeed"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/homeowner/bookings/" + bookingId)
                .header("Authorization", "Bearer " + homeownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("BOOKED")));
  }

  @Test
  void anotherHomeownerCannotCheckoutOrViewSomeoneElsesBooking() throws Exception {
    String bookingId = bookAQuote(new java.math.BigDecimal("10000"));

    mockMvc
        .perform(
            post("/api/v1/homeowner/bookings/" + bookingId + "/checkout")
                .header("Authorization", "Bearer " + otherHomeownerToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            get("/api/v1/homeowner/bookings/" + bookingId)
                .header("Authorization", "Bearer " + otherHomeownerToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void checkoutRejectsABookingThatIsAlreadyBooked() throws Exception {
    String bookingId = bookAQuote(new java.math.BigDecimal("15000"));
    String checkoutJson =
        mockMvc
            .perform(
                post("/api/v1/homeowner/bookings/" + bookingId + "/checkout")
                    .header("Authorization", "Bearer " + homeownerToken))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String transactionId = JsonPath.read(checkoutJson, "$.cmiTransactionId");
    mockMvc
        .perform(post("/api/v1/mock-cmi/" + transactionId + "/succeed"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/homeowner/bookings/" + bookingId + "/checkout")
                .header("Authorization", "Bearer " + homeownerToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void webhookWithoutASignatureHeaderIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"transactionId\":\"x\",\"status\":\"SUCCEEDED\",\"amount\":1,\"currency\":\"MAD\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void webhookWithAnInvalidSignatureIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/payments/webhook")
                .header("X-CMI-Signature", "not-a-real-signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"transactionId\":\"x\",\"status\":\"SUCCEEDED\",\"amount\":1,\"currency\":\"MAD\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void webhookForAnUnknownTransactionIsRejected() throws Exception {
    String body =
        "{\"transactionId\":\"does-not-exist\",\"status\":\"SUCCEEDED\",\"amount\":1,\"currency\":\"MAD\"}";

    mockMvc
        .perform(
            post("/api/v1/payments/webhook")
                .header("X-CMI-Signature", SIGNER.sign(body))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  void webhookWithAMismatchedAmountIsRejected() throws Exception {
    String bookingId = bookAQuote(new java.math.BigDecimal("30000"));
    String checkoutJson =
        mockMvc
            .perform(
                post("/api/v1/homeowner/bookings/" + bookingId + "/checkout")
                    .header("Authorization", "Bearer " + homeownerToken))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String transactionId = JsonPath.read(checkoutJson, "$.cmiTransactionId");
    String body =
        ("{\"transactionId\":\"%s\",\"status\":\"SUCCEEDED\",\"amount\":999999,\"currency\":\"MAD\"}")
            .formatted(transactionId);

    mockMvc
        .perform(
            post("/api/v1/payments/webhook")
                .header("X-CMI-Signature", SIGNER.sign(body))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            get("/api/v1/homeowner/bookings/" + bookingId)
                .header("Authorization", "Bearer " + homeownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("PENDING_PAYMENT")));
  }

  @Test
  void duplicateWebhookDeliveryIsProcessedIdempotently() throws Exception {
    String bookingId = bookAQuote(new java.math.BigDecimal("40000"));
    String checkoutJson =
        mockMvc
            .perform(
                post("/api/v1/homeowner/bookings/" + bookingId + "/checkout")
                    .header("Authorization", "Bearer " + homeownerToken))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String transactionId = JsonPath.read(checkoutJson, "$.cmiTransactionId");
    mockMvc
        .perform(post("/api/v1/mock-cmi/" + transactionId + "/succeed"))
        .andExpect(status().isOk());

    String replayBody =
        ("{\"transactionId\":\"%s\",\"status\":\"SUCCEEDED\",\"amount\":4000,\"currency\":\"MAD\"}")
            .formatted(transactionId);
    mockMvc
        .perform(
            post("/api/v1/payments/webhook")
                .header("X-CMI-Signature", SIGNER.sign(replayBody))
                .contentType(MediaType.APPLICATION_JSON)
                .content(replayBody))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/api/v1/homeowner/bookings/" + bookingId)
                .header("Authorization", "Bearer " + homeownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("BOOKED")));
  }

  /** Homeowner requests a quote from the shared installer, gets quoted, and books it. */
  private String bookAQuote(java.math.BigDecimal quoteAmount) throws Exception {
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
    String email = "homeowner-pay-" + UUID.randomUUID() + "@example.com";
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
