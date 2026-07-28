package com.shamsma.api.installer;

import java.util.UUID;

/**
 * Cross-package view of an installer's identity, for callers (e.g. booking) that only need this.
 */
public record InstallerSummary(
    UUID userId, String businessName, String phone, VerificationStatus verificationStatus) {}
