/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bremersee.eureka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * The in-memory application tests.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.ldap.autoconfigure.LdapAutoConfiguration"
})
@ActiveProfiles({"in-memory"})
@Slf4j
class InMemoryApplicationTests {

  private static final String EUREKA_USER = "eureka";

  private static final String EUREKA_PASS = "changeit";

  private static final String ADMIN_USER = "admin";

  private static final String ADMIN_PASS = "changeit";

  private static final String ACTUATOR_USER = "actuator";

  private static final String ACTUATOR_PASS = "changeit";

  /**
   * The rest template builder.
   */
  @Autowired
  RestTemplateBuilder restTemplateBuilder;

  /**
   * The local server port.
   */
  @LocalServerPort
  int port;

  /**
   * Base url of the local server.
   *
   * @return the base url of the local server
   */
  String baseUrl() {
    return "http://localhost:" + port;
  }

  /**
   * Fetch web.
   */
  @Test
  void fetchWeb() {
    ResponseEntity<String> response = restTemplateBuilder
        .baseUri(baseUrl())
        .basicAuthentication(ADMIN_USER, ADMIN_PASS)
        .build()
        .getForEntity("/", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  /**
   * Fetch web and expect unauthorized.
   */
  @Test
  void fetchWebAndExpectUnauthorized() {
    ResponseEntity<String> response = restTemplateBuilder
        .baseUri(baseUrl())
        .errorHandler(resp -> resp.getStatusCode().is5xxServerError())
        .build()
        .getForEntity("/", String.class);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  /**
   * Fetch apps.
   */
  @Test
  void fetchApps() {
    ResponseEntity<String> response = restTemplateBuilder
        .baseUri(baseUrl())
        .basicAuthentication(EUREKA_USER, EUREKA_PASS)
        .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
        .build()
        .getForEntity("/eureka/apps", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  /**
   * Fetch apps and expect unauthorized.
   */
  @Test
  void fetchAppsAndExpectUnauthorized() {
    ResponseEntity<String> response = restTemplateBuilder
        .baseUri(baseUrl())
        .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
        .errorHandler(resp -> resp.getStatusCode().is5xxServerError())
        .build()
        .getForEntity("/eureka/apps", String.class);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  /**
   * Fetch health.
   */
  @Test
  void fetchHealth() {
    ResponseEntity<String> response = restTemplateBuilder
        .baseUri(baseUrl())
        .build()
        .getForEntity("/actuator/health", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  /**
   * Fetch info.
   */
  @Test
  void fetchInfo() {
    ResponseEntity<String> response = restTemplateBuilder
        .baseUri(baseUrl())
        .build()
        .getForEntity("/actuator/info", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  /**
   * Fetch metrics.
   */
  @Test
  void fetchMetrics() {
    ResponseEntity<String> response = restTemplateBuilder
        .baseUri(baseUrl())
        .basicAuthentication(ACTUATOR_USER, ACTUATOR_PASS)
        .build()
        .getForEntity("/actuator/metrics", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  /**
   * Fetch metrics and expect unauthorized.
   */
  @Test
  void fetchMetricsAndExpectUnauthorized() {
    ResponseEntity<String> response = restTemplateBuilder
        .baseUri(baseUrl())
        .errorHandler(resp -> resp.getStatusCode().is5xxServerError())
        .build()
        .getForEntity("/actuator/metrics", String.class);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

}
