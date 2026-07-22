package com.shamsma.api.auth.dto;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    String role,
    boolean mfaEnrolled) {}
