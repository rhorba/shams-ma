package com.shamsma.api.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** RS256 JWT issuance/validation per Architecture ADR-2 and Security doc section 3. */
@Service
class JwtService {

  static final long ACCESS_TOKEN_TTL_MINUTES = 15;
  static final long REFRESH_TOKEN_TTL_DAYS = 7;

  private final JwtKeyProperties keyProperties;
  private RSAPrivateKey privateKey;
  private RSAPublicKey publicKey;

  JwtService(JwtKeyProperties keyProperties) {
    this.keyProperties = keyProperties;
  }

  @PostConstruct
  void loadKeys() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    this.privateKey = readPrivateKey(keyProperties.privateKeyPath().getInputStream());
    this.publicKey = readPublicKey(keyProperties.publicKeyPath().getInputStream());
  }

  String issueAccessToken(UUID userId, String email, String role, boolean mfaEnrolled) {
    Instant now = Instant.now();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .claim("mfaEnrolled", mfaEnrolled)
            .claim("type", "access")
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(ACCESS_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES)))
            .build();
    return sign(claims);
  }

  String issueRefreshToken(UUID userId) {
    Instant now = Instant.now();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS)))
            .build();
    return sign(claims);
  }

  /** Returns the validated claims, or null if the token is missing/expired/invalid. */
  JWTClaimsSet verify(String token) {
    try {
      SignedJWT signedJwt = SignedJWT.parse(token);
      if (!signedJwt.verify(new RSASSAVerifier(publicKey))) {
        return null;
      }
      JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
      if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
        return null;
      }
      return claims;
    } catch (java.text.ParseException | JOSEException e) {
      return null;
    }
  }

  private String sign(JWTClaimsSet claims) {
    try {
      SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
      signedJwt.sign(new RSASSASigner(privateKey));
      return signedJwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("Failed to sign JWT", e);
    }
  }

  private static RSAPrivateKey readPrivateKey(InputStream in)
      throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    String pem = stripPem(in, "PRIVATE KEY");
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem));
    return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
  }

  private static RSAPublicKey readPublicKey(InputStream in)
      throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    String pem = stripPem(in, "PUBLIC KEY");
    X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(pem));
    return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
  }

  private static String stripPem(InputStream in, String label) throws IOException {
    String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    return content
        .replace("-----BEGIN " + label + "-----", "")
        .replace("-----END " + label + "-----", "")
        .replaceAll("\\s", "");
  }
}
