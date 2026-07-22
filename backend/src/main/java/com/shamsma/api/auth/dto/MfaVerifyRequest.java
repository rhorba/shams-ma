package com.shamsma.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(@NotBlank String code) {}
