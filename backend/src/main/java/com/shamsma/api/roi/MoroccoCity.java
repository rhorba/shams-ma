package com.shamsma.api.roi;

/**
 * A reference city with its approximate average peak sun hours (kWh/m²/day equivalent), used to
 * estimate solar yield for the nearest match to a geocoded address. Figures are MVP ballpark
 * assumptions (Morocco's irradiance genuinely varies regionally, roughly 5.0-6.2 PSH — unlike
 * electricity tariffs, which ONEE applies as a single national bracket schedule, so tariff is NOT
 * modeled per-city here; see RoiCalculator.TARIFF_MAD_PER_KWH).
 */
record MoroccoCity(String name, double lat, double lng, double peakSunHours) {}
