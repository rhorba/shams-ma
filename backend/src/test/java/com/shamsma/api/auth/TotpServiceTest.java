package com.shamsma.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.Test;

class TotpServiceTest {

  private final TotpService totpService = new TotpService();

  @Test
  void generatesUsableSecretAndUri() {
    String secret = totpService.generateSecret();

    assertThat(secret).isNotBlank();
    String uri = totpService.buildOtpAuthUri(secret, "admin@example.com");
    assertThat(uri).startsWith("otpauth://totp/");
    assertThat(uri).contains("Shams.ma");
  }

  @Test
  void verifiesACorrectlyGeneratedCode() throws Exception {
    String secret = totpService.generateSecret();
    long currentBucket = new SystemTimeProvider().getTime() / 30;
    String code = new DefaultCodeGenerator(HashingAlgorithm.SHA1).generate(secret, currentBucket);

    assertThat(totpService.verifyCode(secret, code)).isTrue();
  }

  @Test
  void rejectsWrongCode() {
    String secret = totpService.generateSecret();

    assertThat(totpService.verifyCode(secret, "000000")).isFalse();
  }
}
