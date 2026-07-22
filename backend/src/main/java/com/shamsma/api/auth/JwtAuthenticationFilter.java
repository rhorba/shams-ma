package com.shamsma.api.auth;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the RS256 access token on every request and populates the security context. Admins get
 * an extra ROLE_ADMIN_MFA authority only once MFA is enrolled — that's what gates /api/v1/admin/**
 * (see SecurityConfig), satisfying the "admin without MFA is blocked from dashboard access"
 * acceptance criterion without a separate access-control mechanism.
 */
@Component
class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      JWTClaimsSet claims = jwtService.verify(token);
      if (claims != null && "access".equals(getClaim(claims, "type"))) {
        String role = getClaim(claims, "role");
        boolean mfaEnrolled = Boolean.TRUE.equals(claims.getClaim("mfaEnrolled"));
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        if ("ADMIN".equals(role) && mfaEnrolled) {
          authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN_MFA"));
        }
        var authentication =
            new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }
    chain.doFilter(request, response);
  }

  private static String getClaim(JWTClaimsSet claims, String name) {
    Object value = claims.getClaim(name);
    return value == null ? null : value.toString();
  }
}
