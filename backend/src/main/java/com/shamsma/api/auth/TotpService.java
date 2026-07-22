package com.shamsma.api.auth;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;

@Service
class TotpService {

  private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
  private final TimeProvider timeProvider = new SystemTimeProvider();
  private final CodeVerifier codeVerifier =
      new DefaultCodeVerifier(new DefaultCodeGenerator(HashingAlgorithm.SHA1), timeProvider);

  String generateSecret() {
    return secretGenerator.generate();
  }

  String buildOtpAuthUri(String secret, String email) {
    QrData data =
        new QrData.Builder()
            .label(email)
            .secret(secret)
            .issuer("Shams.ma")
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();
    return data.getUri();
  }

  boolean verifyCode(String secret, String code) {
    return codeVerifier.isValidCode(secret, code);
  }
}
