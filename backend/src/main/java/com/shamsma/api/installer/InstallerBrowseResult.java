package com.shamsma.api.installer;

import java.util.UUID;

public record InstallerBrowseResult(
    UUID userId, String businessName, String phone, double distanceKm) {}
