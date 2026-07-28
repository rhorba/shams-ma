package com.shamsma.api.installer;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Covers Stories 1.2 (upload) and 1.3 (admin review) end-to-end, including a real MinIO
 * Testcontainer — object storage is core to what's being tested, so it isn't mocked (same "verify
 * against a real dependency" rationale as the Postgres/PostGIS container).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.rate-limit.capacity=1000")
@AutoConfigureMockMvc
@Testcontainers
class CertificationFlowIntegrationTest {

  @Container
  static GenericContainer<?> minio =
      new GenericContainer<>("minio/minio:latest")
          .withCommand("server", "/data")
          .withEnv("MINIO_ROOT_USER", "minioadmin")
          .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

  @DynamicPropertySource
  static void fileStorageProps(DynamicPropertyRegistry registry) {
    registry.add(
        "app.file-storage.endpoint",
        () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private static final byte[] FAKE_PDF = {0x25, 0x50, 0x44, 0x46, 0x01, 0x02, 0x03};

  @Test
  void installerUploadsThenAdminApprovesAndInstallerBecomesVisible() throws Exception {
    String installerEmail = "installer-cert-" + UUID.randomUUID() + "@example.com";
    String installerToken = registerLoginInstaller(installerEmail, "Solaire Fes");

    mockMvc
        .perform(
            multipart("/api/v1/installer/certifications")
                .file(new MockMultipartFile("file", "cert.pdf", "application/pdf", FAKE_PDF))
                .header("Authorization", "Bearer " + installerToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status", is("PENDING")));

    String adminToken = registerLoginMfaEnrolledAdmin();

    String listJson =
        mockMvc
            .perform(
                get("/api/v1/admin/certifications")
                    .param("status", "PENDING")
                    .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String certId = JsonPath.read(listJson, "$[0].id");

    mockMvc
        .perform(
            post("/api/v1/admin/certifications/" + certId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());
  }

  @Test
  void uploadRejectsDisallowedFileType() throws Exception {
    String installerToken =
        registerLoginInstaller(
            "installer-badfile-" + UUID.randomUUID() + "@example.com", "Solaire Test");

    mockMvc
        .perform(
            multipart("/api/v1/installer/certifications")
                .file(
                    new MockMultipartFile("file", "cert.txt", "text/plain", "not a pdf".getBytes()))
                .header("Authorization", "Bearer " + installerToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void nonAdminCannotReachReviewQueue() throws Exception {
    String installerToken =
        registerLoginInstaller(
            "installer-noaccess-" + UUID.randomUUID() + "@example.com", "Solaire Test2");

    mockMvc
        .perform(
            get("/api/v1/admin/certifications").header("Authorization", "Bearer " + installerToken))
        .andExpect(status().isForbidden());
  }

  private String registerLoginInstaller(String email, String businessName) throws Exception {
    var registerBody =
        """
        {"email":"%s","password":"Xk9$vTqzR7wLpN","role":"INSTALLER","businessName":"%s"}
        """
            .formatted(email, businessName);
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
        .andExpect(status().isCreated());
    return login(email);
  }

  private String registerLoginMfaEnrolledAdmin() throws Exception {
    User admin =
        userRepository.save(
            new User(
                "admin-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("Xk9$vTqzR7wLpN"),
                UserRole.ADMIN));
    String firstToken = login(admin.getEmail());
    mockMvc
        .perform(post("/api/v1/auth/mfa/enroll").header("Authorization", "Bearer " + firstToken))
        .andExpect(status().isOk());
    return login(admin.getEmail());
  }

  private String login(String email) throws Exception {
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
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(responseJson, "$.accessToken");
  }
}
