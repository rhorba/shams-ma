package com.shamsma.api.roi;

import java.util.List;
import java.util.Map;

/**
 * Pure ROI/payback estimation math — no I/O, no Spring dependencies, so it's directly unit-testable
 * against lat/lng without mocking geocoding. All constants below are documented MVP assumptions (no
 * live pricing/tariff feed exists yet); tune here if real figures become available.
 */
final class RoiCalculator {

  // Single national blended residential rate — ONEE's tariff is a consumption-bracket schedule,
  // not region-varying, so (unlike irradiance) it is not looked up per-city.
  static final double TARIFF_MAD_PER_KWH = 1.2;

  // Inverter/wiring/soiling/temperature derating, typical for a well-maintained residential system.
  static final double PERFORMANCE_RATIO = 0.80;

  // Ballpark installed cost for Morocco residential PV, MAD per kWp.
  static final double COST_PER_KWP_MAD = 12_000.0;

  // Roof area needed per kWp of modern residential panels (~2m² per ~400W panel).
  static final double ROOF_M2_PER_KWP = 6.0;

  private static final double DAYS_PER_MONTH = 30.0;
  private static final double DAYS_PER_YEAR = 365.0;

  private static final Map<RoiOrientation, Double> ORIENTATION_MULTIPLIERS =
      Map.of(
          RoiOrientation.SOUTH, 1.0,
          RoiOrientation.EAST_WEST, 0.85,
          RoiOrientation.NORTH, 0.65,
          RoiOrientation.FLAT_UNKNOWN, 0.9);

  // Approximate average peak sun hours (PSH) by major city — Morocco's irradiance ranges
  // roughly 5.0 (north coast) to 6.4 (deep south); nearest-match is a reasonable MVP proxy for a
  // full solar-atlas lookup.
  private static final List<MoroccoCity> CITIES =
      List.of(
          new MoroccoCity("Tangier", 35.7595, -5.8340, 5.0),
          new MoroccoCity("Tetouan", 35.5785, -5.3684, 5.0),
          new MoroccoCity("Nador", 35.1740, -2.9287, 5.2),
          new MoroccoCity("Oujda", 34.6805, -1.9089, 5.4),
          new MoroccoCity("Fes", 34.0331, -5.0003, 5.3),
          new MoroccoCity("Meknes", 33.8935, -5.5473, 5.4),
          new MoroccoCity("Rabat", 34.0209, -6.8416, 5.2),
          new MoroccoCity("Casablanca", 33.5731, -7.5898, 5.3),
          new MoroccoCity("El Jadida", 33.2316, -8.5007, 5.4),
          new MoroccoCity("Safi", 32.2994, -9.2372, 5.5),
          new MoroccoCity("Marrakech", 31.6295, -7.9811, 5.7),
          new MoroccoCity("Agadir", 30.4278, -9.5981, 5.9),
          new MoroccoCity("Ouarzazate", 30.9335, -6.9370, 6.2),
          new MoroccoCity("Laayoune", 27.1418, -13.1873, 6.4));

  private RoiCalculator() {}

  static RoiEstimateResponse estimate(
      double lat,
      double lng,
      double monthlyBillMad,
      Double roofSizeM2,
      RoiOrientation orientation) {
    MoroccoCity city = nearestCity(lat, lng);
    RoiOrientation effectiveOrientation =
        orientation != null ? orientation : RoiOrientation.FLAT_UNKNOWN;
    double effectivePsh = city.peakSunHours() * ORIENTATION_MULTIPLIERS.get(effectiveOrientation);

    double monthlyKwhUsage = monthlyBillMad / TARIFF_MAD_PER_KWH;
    double annualKwhUsage = monthlyKwhUsage * 12;
    double dailyKwhUsage = monthlyKwhUsage / DAYS_PER_MONTH;

    double demandBasedKwp = dailyKwhUsage / (effectivePsh * PERFORMANCE_RATIO);
    double systemKwp = demandBasedKwp;
    if (roofSizeM2 != null && roofSizeM2 > 0) {
      systemKwp = Math.min(systemKwp, roofSizeM2 / ROOF_M2_PER_KWP);
    }

    double annualProductionKwh = systemKwp * effectivePsh * DAYS_PER_YEAR * PERFORMANCE_RATIO;
    double annualSavingsMad = Math.min(annualProductionKwh, annualKwhUsage) * TARIFF_MAD_PER_KWH;
    double installationCostMad = systemKwp * COST_PER_KWP_MAD;
    Double paybackYears =
        annualSavingsMad > 0 ? round2(installationCostMad / annualSavingsMad) : null;

    return new RoiEstimateResponse(
        lat,
        lng,
        city.name(),
        round2(systemKwp),
        round2(annualProductionKwh),
        round2(installationCostMad),
        round2(annualSavingsMad),
        paybackYears);
  }

  private static MoroccoCity nearestCity(double lat, double lng) {
    return CITIES.stream()
        .min(java.util.Comparator.comparingDouble(c -> haversineKm(lat, lng, c.lat(), c.lng())))
        .orElseThrow();
  }

  private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
    double earthRadiusKm = 6371.0;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2)
                * Math.sin(dLng / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadiusKm * c;
  }

  private static double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
