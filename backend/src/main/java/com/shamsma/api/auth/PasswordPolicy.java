package com.shamsma.api.auth;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Minimum-10-chars + common/breached-password rejection, per Security doc section 3. Backed by a
 * bundled 10k-most-common-passwords list (SecLists, danielmiessler/SecLists — a well-known public
 * breach-derived corpus), checked via an in-memory HashSet. A live k-anonymity API call (e.g.
 * HaveIBeenPwned) would catch more, but requires network access at registration time and is
 * unnecessary for MVP scale.
 */
@Component
class PasswordPolicy {

  private static final int MIN_LENGTH = 10;
  private static final String LIST_RESOURCE = "security/common-passwords.txt";

  private Set<String> commonPasswords = Set.of();

  @PostConstruct
  void loadCommonPasswords() {
    Set<String> loaded = new HashSet<>();
    try (InputStream in = new ClassPathResource(LIST_RESOURCE).getInputStream();
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String trimmed = line.strip().toLowerCase();
        if (!trimmed.isEmpty()) {
          loaded.add(trimmed);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load common-password list", e);
    }
    this.commonPasswords = loaded;
  }

  /** Returns a user-facing rejection reason, or null if the password is acceptable. */
  String reject(String password) {
    if (password == null || password.length() < MIN_LENGTH) {
      return "Password must be at least " + MIN_LENGTH + " characters";
    }
    if (commonPasswords.contains(password.toLowerCase())) {
      return "Password is too common; choose a less predictable password";
    }
    return null;
  }
}
