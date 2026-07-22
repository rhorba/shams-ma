package com.shamsma.api.auth;

import com.shamsma.api.auth.dto.AuthResponse;
import com.shamsma.api.auth.dto.LoginRequest;
import com.shamsma.api.auth.dto.MfaEnrollResponse;
import com.shamsma.api.auth.dto.MfaVerifyRequest;
import com.shamsma.api.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

  private static final String REFRESH_COOKIE_NAME = "refresh_token";

  private final AuthService authService;

  AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
    authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PostMapping("/login")
  ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    LoginResult result = authService.login(request);
    ResponseCookie cookie =
        ResponseCookie.from(REFRESH_COOKIE_NAME, result.refreshToken())
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .path("/api/v1/auth")
            .maxAge(JwtService.REFRESH_TOKEN_TTL_DAYS * 24 * 60 * 60)
            .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(result.response());
  }

  @PostMapping("/mfa/enroll")
  ResponseEntity<MfaEnrollResponse> enrollMfa(Authentication authentication) {
    return ResponseEntity.ok(authService.enrollMfa(currentUserId(authentication)));
  }

  @PostMapping("/mfa/verify")
  ResponseEntity<Map<String, Boolean>> verifyMfa(
      Authentication authentication, @Valid @RequestBody MfaVerifyRequest request) {
    boolean valid = authService.verifyMfaCode(currentUserId(authentication), request.code());
    return ResponseEntity.ok(Map.of("valid", valid));
  }

  private static UUID currentUserId(Authentication authentication) {
    return UUID.fromString((String) authentication.getPrincipal());
  }
}
