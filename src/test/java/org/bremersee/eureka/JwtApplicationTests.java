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
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * The jwt application tests.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwt/auth",
    "bremersee.auth.password-flow.token-endpoint=http://localhost/jwt/token",
    "bremersee.auth.password-flow.client-id=changeit",
    "bremersee.auth.password-flow.client-secret=changeit"
})
@TestInstance(Lifecycle.PER_CLASS) // allows us to use @BeforeAll with a non-static method
public class JwtApplicationTests {

  private static final String eurekaUser = "eureka";

  private static final String eurekaPass = "eureka";

  /**
   * The rest template.
   */
  @Autowired
  TestRestTemplate restTemplate;

  /**
   * The context.
   */
  @Autowired
  WebApplicationContext context;

  /**
   * The mock mvc.
   */
  MockMvc mvc;

  /**
   * Setup mock mvc.
   */
  @BeforeAll
  void setup() {
    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  /**
   * Fetch web.
   */
  @Test
  @WithMockUser(username = "admin", password = "admin", authorities = {"ROLE_ADMIN"})
  void fetchWeb() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk());
  }

  /**
   * Fetch web.
   */
  @Test
  @WithMockUser(username = "user", password = "user", authorities = {"ROLE_USER"})
  void fetchWebAndExpectForbidden() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isForbidden());
  }

  /**
   * Fetch apps.
   */
  @Test
  void fetchApps() {
    ResponseEntity<String> response = restTemplate
        .withBasicAuth(eurekaUser, eurekaPass)
        .getForEntity("/eureka/apps", String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  /**
   * Fetch apps and expect unauthorized.
   */
  @Test
  void fetchAppsAndExpectUnauthorized() {
    ResponseEntity<String> response = restTemplate
        .getForEntity("/eureka/apps", String.class);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

}
