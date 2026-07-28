package com.shamsma.api.booking;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record QuoteRespondRequest(
    @NotNull QuoteAction action, BigDecimal quoteAmount, @Size(max = 2000) String quoteNotes) {}
