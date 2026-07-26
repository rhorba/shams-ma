package com.shamsma.api.installer;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public browse endpoint — no auth, per Security doc's rate-limited-public-endpoint pattern. */
@RestController
@RequestMapping("/api/v1/installers")
class InstallerBrowseController {

  private final InstallerService installerService;

  InstallerBrowseController(InstallerService installerService) {
    this.installerService = installerService;
  }

  @GetMapping("/browse")
  List<InstallerBrowseResult> browse(@RequestParam double lat, @RequestParam double lng) {
    return installerService.browse(lat, lng);
  }
}
