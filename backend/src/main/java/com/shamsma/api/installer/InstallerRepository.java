package com.shamsma.api.installer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface InstallerRepository extends JpaRepository<Installer, UUID> {

  /**
   * Sets the coverage-zone center/radius via native SQL, bypassing JPA entity mapping — same "skip
   * hibernate-spatial/JTS" approach as migration V10 (see Story 0.3 decisions log): {@code
   * base_location} stays intentionally unmapped on {@link Installer}.
   */
  @Modifying
  @Transactional
  @Query(
      value =
          "UPDATE installers SET "
              + "base_location = ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, "
              + "coverage_radius_km = :radiusKm, "
              + "updated_at = now() "
              + "WHERE user_id = :userId",
      nativeQuery = true)
  int updateCoverageZone(
      @Param("userId") UUID userId,
      @Param("lat") double lat,
      @Param("lng") double lng,
      @Param("radiusKm") BigDecimal radiusKm);

  @Query(
      value =
          "SELECT user_id AS userId, business_name AS businessName, phone AS phone, "
              + "ST_Distance(base_location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) / 1000.0 "
              + "AS distanceKm "
              + "FROM installers "
              + "WHERE verification_status = 'APPROVED' "
              + "AND base_location IS NOT NULL AND coverage_radius_km IS NOT NULL "
              + "AND ST_DWithin(base_location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, "
              + "coverage_radius_km * 1000) "
              + "ORDER BY distanceKm ASC",
      nativeQuery = true)
  List<InstallerBrowseRow> findWithinRadius(@Param("lat") double lat, @Param("lng") double lng);
}
