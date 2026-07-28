package com.shamsma.api.booking;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Self-service lookup for the authenticated homeowner's own booking. */
@RestController
@RequestMapping("/api/v1/homeowner/bookings")
class BookingController {

  private final BookingService bookingService;

  BookingController(BookingService bookingService) {
    this.bookingService = bookingService;
  }

  @GetMapping("/{id}")
  BookingResponse get(Authentication authentication, @PathVariable UUID id) {
    return bookingService.getOwnedBooking(currentUserId(authentication), id);
  }

  private static UUID currentUserId(Authentication authentication) {
    return UUID.fromString((String) authentication.getPrincipal());
  }
}
