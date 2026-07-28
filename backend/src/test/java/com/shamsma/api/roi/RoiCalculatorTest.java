package com.shamsma.api.roi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class RoiCalculatorTest {

  // Exact Rabat city coordinates from RoiCalculator's table.
  private static final double RABAT_LAT = 34.0209;
  private static final double RABAT_LNG = -6.8416;

  @Test
  void resolvesTheNearestCityByCoordinates() {
    RoiEstimateResponse result =
        RoiCalculator.estimate(RABAT_LAT, RABAT_LNG, 500, null, RoiOrientation.SOUTH);

    assertThat(result.resolvedCity()).isEqualTo("Rabat");
  }

  @Test
  void unsizedSystemAlwaysFullyCoversAnnualUsage() {
    // With no roof constraint, the system is demand-sized to cover usage regardless of location
    // or orientation, so annual savings always equal monthlyBillMad * 12 exactly (the small
    // day-count rounding in the sizing formula always overshoots slightly, never undershoots).
    for (RoiOrientation orientation : RoiOrientation.values()) {
      RoiEstimateResponse result =
          RoiCalculator.estimate(RABAT_LAT, RABAT_LNG, 500, null, orientation);
      assertThat(result.annualSavingsMad()).isCloseTo(6000.0, within(0.01));
      assertThat(result.paybackYears()).isNotNull().isPositive();
    }
  }

  @Test
  void worseOrientationNeedsABiggerSystemForTheSamePaybackInput() {
    RoiEstimateResponse south =
        RoiCalculator.estimate(RABAT_LAT, RABAT_LNG, 500, null, RoiOrientation.SOUTH);
    RoiEstimateResponse north =
        RoiCalculator.estimate(RABAT_LAT, RABAT_LNG, 500, null, RoiOrientation.NORTH);

    assertThat(north.estimatedSystemKwp()).isGreaterThan(south.estimatedSystemKwp());
    assertThat(north.installationCostMad()).isGreaterThan(south.installationCostMad());
    // Same annual savings (both fully cover usage) but north costs more to install -> longer
    // payback.
    assertThat(north.paybackYears()).isGreaterThan(south.paybackYears());
  }

  @Test
  void nullOrientationDefaultsToFlatUnknown() {
    RoiEstimateResponse defaulted = RoiCalculator.estimate(RABAT_LAT, RABAT_LNG, 500, null, null);
    RoiEstimateResponse explicit =
        RoiCalculator.estimate(RABAT_LAT, RABAT_LNG, 500, null, RoiOrientation.FLAT_UNKNOWN);

    assertThat(defaulted).isEqualTo(explicit);
  }

  @Test
  void smallRoofCapsTheSystemBelowDemandSizing() {
    RoiEstimateResponse uncapped =
        RoiCalculator.estimate(RABAT_LAT, RABAT_LNG, 500, null, RoiOrientation.SOUTH);
    RoiEstimateResponse roofCapped =
        RoiCalculator.estimate(RABAT_LAT, RABAT_LNG, 500, 6.0, RoiOrientation.SOUTH);

    // 6 m² of roof / 6 m² per kWp = exactly 1.0 kWp, well below the uncapped demand-sized system.
    assertThat(roofCapped.estimatedSystemKwp()).isEqualTo(1.0);
    assertThat(roofCapped.estimatedSystemKwp()).isLessThan(uncapped.estimatedSystemKwp());
    // A roof-capped system produces less than full usage, so savings are less than the 500*12 cap.
    assertThat(roofCapped.annualSavingsMad()).isLessThan(uncapped.annualSavingsMad());
  }

  @Test
  void zeroBillProducesNoViableSystemAndNullPayback() {
    RoiEstimateResponse result =
        RoiCalculator.estimate(RABAT_LAT, RABAT_LNG, 0, null, RoiOrientation.SOUTH);

    assertThat(result.estimatedSystemKwp()).isZero();
    assertThat(result.annualSavingsMad()).isZero();
    assertThat(result.paybackYears()).isNull();
  }

  @Test
  void farSouthCoordinatesResolveToLaayoune() {
    RoiEstimateResponse result =
        RoiCalculator.estimate(27.15, -13.2, 500, null, RoiOrientation.SOUTH);

    assertThat(result.resolvedCity()).isEqualTo("Laayoune");
  }
}
