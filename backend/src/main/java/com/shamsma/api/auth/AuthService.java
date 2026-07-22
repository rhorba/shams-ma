package com.shamsma.api.auth;

import com.shamsma.api.auth.dto.AuthResponse;
import com.shamsma.api.auth.dto.LoginRequest;
import com.shamsma.api.auth.dto.MfaEnrollResponse;
import com.shamsma.api.auth.dto.RegisterRequest;
import com.shamsma.api.homeowner.HomeownerService;
import com.shamsma.api.installer.InstallerService;
import com.shamsma.api.shared.User;
import com.shamsma.api.shared.UserRepository;
import com.shamsma.api.shared.UserRole;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class AuthService {

  private final UserRepository userRepository;
  private final HomeownerService homeownerService;
  private final InstallerService installerService;
  private final PasswordEncoder passwordEncoder;
  private final PasswordPolicy passwordPolicy;
  private final JwtService jwtService;
  private final TotpService totpService;

  AuthService(
      UserRepository userRepository,
      HomeownerService homeownerService,
      InstallerService installerService,
      PasswordEncoder passwordEncoder,
      PasswordPolicy passwordPolicy,
      JwtService jwtService,
      TotpService totpService) {
    this.userRepository = userRepository;
    this.homeownerService = homeownerService;
    this.installerService = installerService;
    this.passwordEncoder = passwordEncoder;
    this.passwordPolicy = passwordPolicy;
    this.jwtService = jwtService;
    this.totpService = totpService;
  }

  void register(RegisterRequest request) {
    String rejection = passwordPolicy.reject(request.password());
    if (rejection != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, rejection);
    }
    if (userRepository.existsByEmail(request.email())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    }

    UserRole role = UserRole.valueOf(request.role().name());
    User user = new User(request.email(), passwordEncoder.encode(request.password()), role);
    user = userRepository.save(user);

    if (role == UserRole.HOMEOWNER) {
      requireNonBlank(request.fullName(), "fullName");
      requireNonBlank(request.addressText(), "addressText");
      homeownerService.createProfile(
          user.getId(), request.fullName(), request.phone(), request.addressText());
    } else {
      requireNonBlank(request.businessName(), "businessName");
      installerService.createProfile(user.getId(), request.businessName(), request.phone());
    }
  }

  LoginResult login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    boolean mfaEnrolled = user.isMfaEnrolled();
    String accessToken =
        jwtService.issueAccessToken(
            user.getId(), user.getEmail(), user.getRole().name(), mfaEnrolled);
    String refreshToken = jwtService.issueRefreshToken(user.getId());

    AuthResponse response =
        new AuthResponse(
            accessToken,
            "Bearer",
            JwtService.ACCESS_TOKEN_TTL_MINUTES * 60,
            user.getRole().name(),
            mfaEnrolled);
    return new LoginResult(response, refreshToken);
  }

  MfaEnrollResponse enrollMfa(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if (user.getRole() != UserRole.ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MFA enrollment is admin-only");
    }
    String secret = totpService.generateSecret();
    user.setMfaSecret(secret);
    userRepository.save(user);
    return new MfaEnrollResponse(secret, totpService.buildOtpAuthUri(secret, user.getEmail()));
  }

  boolean verifyMfaCode(UUID userId, String code) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if (user.getMfaSecret() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA not enrolled");
    }
    return totpService.verifyCode(user.getMfaSecret(), code);
  }

  private static void requireNonBlank(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
    }
  }
}
