package com.shamsma.api.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/**
 * Stands in for CMI's hosted checkout page — see {@link MockCmiCheckoutServiceImpl}. Builds a real,
 * correctly-signed webhook payload and calls the exact same {@link PaymentService#processWebhook}
 * that a genuine CMI callback would hit, so only the trigger (a button in our own UI instead of a
 * bank redirect) is mocked.
 */
@RestController
@RequestMapping("/api/v1/mock-cmi")
class MockCmiController {

  private final PaymentRepository paymentRepository;
  private final CmiSignatureService cmiSignatureService;
  private final PaymentService paymentService;
  private final ObjectMapper objectMapper;

  MockCmiController(
      PaymentRepository paymentRepository,
      CmiSignatureService cmiSignatureService,
      PaymentService paymentService,
      ObjectMapper objectMapper) {
    this.paymentRepository = paymentRepository;
    this.cmiSignatureService = cmiSignatureService;
    this.paymentService = paymentService;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/{transactionId}/succeed")
  ResponseEntity<Void> succeed(@PathVariable String transactionId) {
    simulate(transactionId, "SUCCEEDED");
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{transactionId}/fail")
  ResponseEntity<Void> fail(@PathVariable String transactionId) {
    simulate(transactionId, "FAILED");
    return ResponseEntity.ok().build();
  }

  private void simulate(String transactionId, String status) {
    Payment payment =
        paymentRepository
            .findByCmiTransactionId(transactionId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown mock transaction"));
    String rawBody =
        objectMapper.writeValueAsString(
            new CmiWebhookPayload(
                transactionId, status, payment.getAmount(), payment.getCurrency()));
    paymentService.processWebhook(rawBody, cmiSignatureService.sign(rawBody));
  }
}
