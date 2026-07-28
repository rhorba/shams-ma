package com.shamsma.api.auth;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Stateless JWT security config per Architecture ADR-2. */
@Configuration
@EnableWebSecurity
class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RateLimitFilter rateLimitFilter;
  private final List<String> allowedOrigins;

  SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      RateLimitFilter rateLimitFilter,
      @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.rateLimitFilter = rateLimitFilter;
    this.allowedOrigins = allowedOrigins;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/auth/register", "/api/v1/auth/login")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/error")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/installers/browse")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/roi/estimate")
                    .permitAll()
                    .requestMatchers("/api/v1/payments/webhook", "/api/v1/mock-cmi/**")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/mfa/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN_MFA")
                    .requestMatchers("/api/v1/installer/**")
                    .hasRole("INSTALLER")
                    .requestMatchers("/api/v1/homeowner/**")
                    .hasRole("HOMEOWNER")
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  private CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
