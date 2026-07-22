package com.shamsma.api.auth.dto;

public record MfaEnrollResponse(String secret, String otpAuthUri) {}
