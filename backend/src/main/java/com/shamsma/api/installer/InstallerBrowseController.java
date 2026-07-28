package com.shamsma.api.installer;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Public browse endpoint — no auth, per Security doc's rate-limited-public-endpoint pattern. */
@RestController
@RequestMapping("/api/v1/installers")
class InstallerBrowseController {

  private final InstallerService installerService;

  InstallerBrowseController(InstallerService installerService) {
    this.installerService = installerService;
  }

  /** Accepts either a free-text {@code address} (geocoded server-side) or an explicit lat/lng. */
  @GetMapping("/browse")
  List<InstallerBrowseResult> browse(
      @RequestParam(required = false) String address,
      @RequestParam(required = false) Double lat,
      @RequestParam(required = false) Double lng) {
    if (address != null && !address.isBlank()) {
      return installerService.browseByAddress(address);
    }
    if (lat == null || lng == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Provide either address or both lat and lng");
    }
    return installerService.browse(lat, lng);
  }
}
