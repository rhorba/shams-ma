package com.shamsma.api.payment;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** HMAC-SHA256 request signing/verification against the shared {@code CMI_SECRET}. */
@Component
class CmiSignatureService {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final byte[] secretKeyBytes;

  CmiSignatureService(@Value("${app.cmi.secret}") String secret) {
    this.secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
  }

  String sign(String payload) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(secretKeyBytes, HMAC_ALGORITHM));
      return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("Could not compute CMI signature", e);
    }
  }

  /** Constant-time comparison — avoids leaking signature-match progress via response timing. */
  boolean verify(String payload, String signature) {
    if (signature == null) {
      return false;
    }
    String expected = sign(payload);
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
  }
}
