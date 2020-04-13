package org.bremersee.eureka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "bremersee.access.application-access=hasIpAddress('213.136.81.244')", // disable local access
    "bremersee.access.admin-user-name=testadmin",
    "bremersee.access.admin-user-password=pass4admin",
    "bremersee.access.actuator-access=hasIpAddress('213.136.81.244')", // disable local access
    "bremersee.access.actuator-user-name=testactuator",
    "bremersee.access.actuator-user-password=pass4actuator"
})
@TestInstance(Lifecycle.PER_CLASS) // allows us to use @BeforeAll with a non-static method
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApplicationTests {

  private static final String adminUser = "testadmin";

  private static final String adminPass = "pass4admin";

  private static final String actuatorUser = "testactuator";

  private static final String actuatorPass = "pass4actuator";

  @Autowired
  TestRestTemplate restTemplate;

  @Test
  void fetchApps() {
    ResponseEntity<String> response = restTemplate
        .withBasicAuth(adminUser, adminPass)
        .getForEntity("/eureka/apps", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  @Test
  void fetchAppsAndExpectUnauthorized() {
    ResponseEntity<String> response = restTemplate
        .getForEntity("/eureka/apps", String.class);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void fetchInfo() {
    ResponseEntity<String> response = restTemplate
        .getForEntity("/actuator/info", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    System.out.println("==> " + response.getBody());
  }

  @Test
  void fetchMetrics() {
    ResponseEntity<String> response = restTemplate
        .withBasicAuth(actuatorUser, actuatorPass)
        .getForEntity("/actuator/metrics", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    System.out.println("==> " + response.getBody());
  }

  @Test
  void fetchMetricsAndExpectUnauthorized() {
    ResponseEntity<String> response = restTemplate
        .getForEntity("/actuator/metrics", String.class);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

}
