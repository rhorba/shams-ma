package com.shamsma.api.booking;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Self-service endpoints for the authenticated installer's own lead inbox. */
@RestController
@RequestMapping("/api/v1/installer/quote-requests")
class InstallerQuoteController {

  private final QuoteRequestService quoteRequestService;

  InstallerQuoteController(QuoteRequestService quoteRequestService) {
    this.quoteRequestService = quoteRequestService;
  }

  @GetMapping
  List<QuoteRequestSummary> myLeads(Authentication authentication) {
    return quoteRequestService.listForInstaller(currentUserId(authentication));
  }

  @PostMapping("/{id}/respond")
  QuoteRequestSummary respond(
      Authentication authentication,
      @PathVariable UUID id,
      @Valid @RequestBody QuoteRespondRequest request) {
    return quoteRequestService.respond(
        currentUserId(authentication),
        id,
        request.action(),
        request.quoteAmount(),
        request.quoteNotes());
  }

  private static UUID currentUserId(Authentication authentication) {
    return UUID.fromString((String) authentication.getPrincipal());
  }
}
