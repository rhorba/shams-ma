package com.shamsma.api.auth;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.shamsma.api.TestcontainersConfiguration;
import com.shamsma.api.shared.User;
import com.shamsma.api.shared.UserRepository;
import com.shamsma.api.shared.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void registerThenLoginHomeownerHappyPath() throws Exception {
    String email = "homeowner-" + UUID.randomUUID() + "@example.com";
    var registerBody =
        """
        {"email":"%s","password":"Xk9$vTqzR7wLpN","role":"HOMEOWNER",
         "fullName":"Amina Bennani","phone":"+212600000000","addressText":"Rabat, Morocco"}
        """
            .formatted(email);

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
        .andExpect(status().isCreated());

    var loginBody =
        """
        {"email":"%s","password":"Xk9$vTqzR7wLpN"}
        """
            .formatted(email);

    mockMvc
        .perform(
            post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role", is("HOMEOWNER")))
        .andExpect(jsonPath("$.mfaEnrolled", is(false)))
        .andExpect(jsonPath("$.accessToken").exists());
  }

  @Test
  void registerRejectsWeakPassword() throws Exception {
    var body =
        """
        {"email":"weak@example.com","password":"short","role":"HOMEOWNER",
         "fullName":"X","addressText":"Rabat"}
        """;

    mockMvc
        .perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void registerRejectsDuplicateEmail() throws Exception {
    String email = "dup-" + UUID.randomUUID() + "@example.com";
    var body =
        """
        {"email":"%s","password":"Xk9$vTqzR7wLpN","role":"INSTALLER",
         "businessName":"Solaire Atlas"}
        """
            .formatted(email);

    mockMvc
        .perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void loginRejectsWrongPassword() throws Exception {
    String email = "wrongpw-" + UUID.randomUUID() + "@example.com";
    var registerBody =
        """
        {"email":"%s","password":"Xk9$vTqzR7wLpN","role":"INSTALLER","businessName":"Solaire Nord"}
        """
            .formatted(email);
    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerBody));

    var loginBody =
        """
        {"email":"%s","password":"WrongPassword1!"}
        """
            .formatted(email);

    mockMvc
        .perform(
            post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void nonAdminIsForbiddenFromAdminRoutes() throws Exception {
    String email = "homeowner2-" + UUID.randomUUID() + "@example.com";
    var registerBody =
        """
        {"email":"%s","password":"Xk9$vTqzR7wLpN","role":"HOMEOWNER",
         "fullName":"Youssef Alami","addressText":"Casablanca"}
        """
            .formatted(email);
    mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(registerBody));

    var loginBody =
        """
        {"email":"%s","password":"Xk9$vTqzR7wLpN"}
        """
            .formatted(email);
    String responseJson =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String accessToken = JsonPath.read(responseJson, "$.accessToken");

    mockMvc
        .perform(
            get("/api/v1/admin/certifications").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminWithoutMfaEnrollmentIsBlockedFromAdminRoutes() throws Exception {
    User admin =
        userRepository.save(
            new User(
                "admin-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("Xk9$vTqzR7wLpN"),
                UserRole.ADMIN));

    var loginBody =
        """
        {"email":"%s","password":"Xk9$vTqzR7wLpN"}
        """
            .formatted(admin.getEmail());
    String responseJson =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mfaEnrolled", is(false)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String accessToken = JsonPath.read(responseJson, "$.accessToken");

    mockMvc
        .perform(
            get("/api/v1/admin/certifications").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(post("/api/v1/auth/mfa/enroll").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.secret").exists())
        .andExpect(jsonPath("$.otpAuthUri").exists());
  }
}
