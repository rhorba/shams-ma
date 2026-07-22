package com.shamsma.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jwt.JWTClaimsSet;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class JwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  void setUp() throws Exception {
    JwtKeyProperties keyProperties =
        new JwtKeyProperties(
            new ClassPathResource("keys/private.pem"), new ClassPathResource("keys/public.pem"));
    jwtService = new JwtService(keyProperties);
    jwtService.loadKeys();
  }

  @Test
  void issuesAndVerifiesAccessToken() {
    UUID userId = UUID.randomUUID();
    String token = jwtService.issueAccessToken(userId, "a@example.com", "HOMEOWNER", false);

    JWTClaimsSet claims = jwtService.verify(token);

    assertThat(claims).isNotNull();
    assertThat(claims.getSubject()).isEqualTo(userId.toString());
    assertThat(claims.getClaim("role")).isEqualTo("HOMEOWNER");
    assertThat(claims.getClaim("mfaEnrolled")).isEqualTo(false);
    assertThat(claims.getClaim("type")).isEqualTo("access");
  }

  @Test
  void issuesAndVerifiesRefreshToken() {
    UUID userId = UUID.randomUUID();
    String token = jwtService.issueRefreshToken(userId);

    JWTClaimsSet claims = jwtService.verify(token);

    assertThat(claims).isNotNull();
    assertThat(claims.getSubject()).isEqualTo(userId.toString());
    assertThat(claims.getClaim("type")).isEqualTo("refresh");
  }

  @Test
  void rejectsTamperedToken() {
    String token = jwtService.issueAccessToken(UUID.randomUUID(), "a@example.com", "ADMIN", true);
    String tampered = token.substring(0, token.length() - 4) + "abcd";

    assertThat(jwtService.verify(tampered)).isNull();
  }

  @Test
  void rejectsGarbageToken() {
    assertThat(jwtService.verify("not-a-jwt")).isNull();
  }
}
