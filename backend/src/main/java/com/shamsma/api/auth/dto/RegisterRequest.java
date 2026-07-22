package com.shamsma.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Single registration payload for both self-registerable roles (HOMEOWNER, INSTALLER). ADMIN
 * accounts are provisioned out-of-band (not via open self-registration) given the elevated
 * privileges — not an explicit doc requirement, but a standard/expected constraint for an admin
 * role with MFA-gated access to payment/booking data.
 */
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotNull Role role,
    // Homeowner fields
    String fullName,
    String phone,
    String addressText,
    // Installer fields
    String businessName) {

  public enum Role {
    HOMEOWNER,
    INSTALLER
  }
}
