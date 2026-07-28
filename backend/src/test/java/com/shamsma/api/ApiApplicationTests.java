package com.shamsma.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.rate-limit.capacity=1000")
class ApiApplicationTests {

  @Test
  void contextLoads() {}
}
