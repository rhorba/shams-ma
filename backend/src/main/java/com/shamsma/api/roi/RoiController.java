package com.shamsma.api.roi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public ROI/payback calculator — no auth, per PRD NFR-2. */
@RestController
@RequestMapping("/api/v1/roi")
@Validated
class RoiController {

  private final RoiService roiService;

  RoiController(RoiService roiService) {
    this.roiService = roiService;
  }

  @GetMapping("/estimate")
  RoiEstimateResponse estimate(
      @RequestParam @NotBlank String address,
      @RequestParam @Positive double monthlyBillMad,
      @RequestParam(required = false) @Positive Double roofSizeM2,
      @RequestParam(required = false) RoiOrientation orientation) {
    return roiService.estimate(address, monthlyBillMad, roofSizeM2, orientation);
  }
}
