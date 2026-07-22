package com.shamsma.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  public PostgreSQLContainer postgresContainer() {
    // Must be postgis/postgis, not plain postgres — V1__enable_extensions.sql requires the
    // PostGIS extension to be installable, and matches the image used by docker-compose.yml.
    return new PostgreSQLContainer(
        DockerImageName.parse("postgis/postgis:16-3.4-alpine")
            .asCompatibleSubstituteFor("postgres"));
  }
}
