package com.shamsma.api.roi;

/**
 * @param paybackYears null when the estimated system never pays for itself at the given bill
 *     (annual savings <= 0) — the frontend shows a "not cost-effective at this bill level" message
 *     instead of a bogus number.
 */
public record RoiEstimateResponse(
    double lat,
    double lng,
    String resolvedCity,
    double estimatedSystemKwp,
    double annualProductionKwh,
    double installationCostMad,
    double annualSavingsMad,
    Double paybackYears) {}
