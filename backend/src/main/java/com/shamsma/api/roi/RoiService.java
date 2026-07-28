package com.shamsma.api.roi;

interface RoiService {

  RoiEstimateResponse estimate(
      String address, double monthlyBillMad, Double roofSizeM2, RoiOrientation orientation);
}
