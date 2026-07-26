package com.shamsma.api.installer;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CoverageZoneRequest(
    @NotBlank String addressText,
    @NotNull @DecimalMin("1") @DecimalMax("200") BigDecimal radiusKm) {}
