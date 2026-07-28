package com.shamsma.api.homeowner;

import java.util.UUID;

/** Cross-package view of a homeowner's identity, for callers (e.g. booking) that only need this. */
public record HomeownerSummary(UUID userId, String fullName, String phone) {}
