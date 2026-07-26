package com.shamsma.api.installer;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record CertificationSummary(
    UUID id,
    UUID installerId,
    String businessName,
    VerificationStatus status,
    URI viewUrl,
    Instant uploadedAt) {}
