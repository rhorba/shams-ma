package com.shamsma.api.auth;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * In-memory per-IP rate limiting on login/registration, per Security doc section 6. Single-instance
 * MVP scale (per System Design) — no Redis/distributed store needed; revisit if we ever run
 * multiple API replicas.
 *
 * <p>Capacity/refill period are configurable (defaults match production: 10/min) because this
 * bucket is a singleton in-memory map shared by every test class in the same cached Spring test
 * context — enough integration tests hitting /api/v1/auth/register or /api/v1/payments/webhook
 * across the whole suite will otherwise legitimately exhaust the real production budget and 429.
 * Tests override via {@code @SpringBootTest(properties = "app.rate-limit.capacity=1000")}.
 */
@Component
class RateLimitFilter extends OncePerRequestFilter {

  private static final Set<String> LIMITED_PATHS =
      Set.of(
          "/api/v1/auth/login",
          "/api/v1/auth/register",
          "/api/v1/installers/browse",
          "/api/v1/roi/estimate",
          "/api/v1/payments/webhook");

  private final int capacity;
  private final Duration refillPeriod;
  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  RateLimitFilter(
      @Value("${app.rate-limit.capacity:10}") int capacity,
      @Value("${app.rate-limit.refill-period-seconds:60}") long refillPeriodSeconds) {
    this.capacity = capacity;
    this.refillPeriod = Duration.ofSeconds(refillPeriodSeconds);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    if (LIMITED_PATHS.contains(path)) {
      // Keyed by client + path (not client alone) so hammering one public endpoint (e.g. the ROI
      // calculator) can't also exhaust an unrelated endpoint's budget (e.g. login) for the same
      // IP/NAT.
      Bucket bucket = buckets.computeIfAbsent(clientKey(request) + ":" + path, key -> newBucket());
      if (!bucket.tryConsume(1)) {
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Too many requests, try again later\"}");
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private Bucket newBucket() {
    Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, refillPeriod));
    return Bucket.builder().addLimit(limit).build();
  }

  private static String clientKey(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    return forwardedFor != null ? forwardedFor.split(",")[0].strip() : request.getRemoteAddr();
  }
}
