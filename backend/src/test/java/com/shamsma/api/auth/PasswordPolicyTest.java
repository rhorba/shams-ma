package com.shamsma.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

  private PasswordPolicy policy;

  @BeforeEach
  void setUp() {
    policy = new PasswordPolicy();
    policy.loadCommonPasswords();
  }

  @Test
  void rejectsShortPasswords() {
    assertThat(policy.reject("Sh0rt!ab")).isNotNull();
  }

  @Test
  void rejectsCommonPasswords() {
    assertThat(policy.reject("qwertyuiop")).isNotNull();
  }

  @Test
  void acceptsStrongUncommonPassword() {
    assertThat(policy.reject("Xk9$vTqzR7wLpN")).isNull();
  }
}
