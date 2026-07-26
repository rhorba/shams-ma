package com.shamsma.api.installer;

import java.util.UUID;

/**
 * Spring Data JPA interface projection for the native browse query — avoids mapping the geography
 * columns on the {@link Installer} entity (see the class-level Javadoc there).
 */
interface InstallerBrowseRow {
  UUID getUserId();

  String getBusinessName();

  String getPhone();

  Double getDistanceKm();
}
