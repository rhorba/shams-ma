package com.shamsma.api.booking;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuoteRequestCreateRequest(
    @NotEmpty List<UUID> installerIds,
    @Size(max = 2000) String message,
    BigDecimal roiEstimateKwh,
    BigDecimal roiPaybackYears) {}
