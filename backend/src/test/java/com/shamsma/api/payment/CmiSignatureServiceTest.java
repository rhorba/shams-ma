package com.shamsma.api.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CmiSignatureServiceTest {

  private final CmiSignatureService service = new CmiSignatureService("test-secret");

  @Test
  void verifyAcceptsASignatureItProduced() {
    String payload = "{\"transactionId\":\"t1\",\"status\":\"SUCCEEDED\"}";
    String signature = service.sign(payload);

    assertThat(service.verify(payload, signature)).isTrue();
  }

  @Test
  void verifyRejectsATamperedPayload() {
    String signature = service.sign("{\"amount\":100}");

    assertThat(service.verify("{\"amount\":999}", signature)).isFalse();
  }

  @Test
  void verifyRejectsAWrongSignature() {
    assertThat(service.verify("{\"amount\":100}", "not-the-right-signature")).isFalse();
  }

  @Test
  void verifyRejectsANullSignature() {
    assertThat(service.verify("{\"amount\":100}", null)).isFalse();
  }

  @Test
  void differentSecretsProduceDifferentSignatures() {
    String payload = "{\"amount\":100}";
    CmiSignatureService otherService = new CmiSignatureService("a-different-secret");

    assertThat(service.sign(payload)).isNotEqualTo(otherService.sign(payload));
  }
}
