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

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * The web security properties.
 *
 * @author Christian Bremer
 */
@ConfigurationProperties(prefix = "bremersee.access")
@Getter
@Setter
@ToString(exclude = {"clientUserPassword", "actuatorUserPassword", "adminUserPassword"})
@EqualsAndHashCode
@NoArgsConstructor
@Slf4j
public class WebSecurityProperties {

  private static final String ROLE_APPLICATION = "ROLE_EUREKA_CLIENT";

  private static final String ROLE_ACTUATOR = "ROLE_ACTUATOR";

  private static final String ROLE_APPLICATION_EXPRESSION =
      "hasAuthority('" + ROLE_APPLICATION + "')";

  private static final String ROLE_ACTUATOR_EXPRESSION = "hasAuthority('" + ROLE_ACTUATOR + "')";

  private String applicationAccess =
      "hasIpAddress('127.0.0.1/32') or hasIpAddress('::1') or " + ROLE_APPLICATION_EXPRESSION;

  private String actuatorAccess =
      "hasIpAddress('127.0.0.1/32') or hasIpAddress('::1') or " + ROLE_ACTUATOR_EXPRESSION;

  private String clientUserName;

  private String clientUserPassword;

  private String actuatorUserName;

  private String actuatorUserPassword;

  private String adminUserName;

  private String adminUserPassword;

  /**
   * Build application access expression.
   *
   * @return the string
   */
  String buildApplicationAccess() {
    return buildAccess(applicationAccess, ROLE_APPLICATION_EXPRESSION);
  }

  /**
   * Build actuator access expression.
   *
   * @return the string
   */
  String buildActuatorAccess() {
    return buildAccess(actuatorAccess, ROLE_ACTUATOR_EXPRESSION);
  }

  private String buildAccess(final String access, final String expression) {
    if (!StringUtils.hasText(access)) {
      return expression;
    } else if (!access.contains(expression)) {
      return access + " or " + expression;
    }
    return access;
  }

  /**
   * Build users.
   *
   * @return the users
   */
  List<SimpleUser> buildUsers() {
    List<SimpleUser> users = new ArrayList<>(3);
    if (StringUtils.hasText(clientUserName)) {
      users.add(new SimpleUser(clientUserName, clientUserPassword, ROLE_APPLICATION));
    }
    if (StringUtils.hasText(actuatorUserName)) {
      users.add(new SimpleUser(actuatorUserName, actuatorUserPassword, ROLE_ACTUATOR));
    }
    if (StringUtils.hasText(adminUserName)) {
      users.add(new SimpleUser(adminUserName, adminUserPassword, ROLE_APPLICATION, ROLE_ACTUATOR));
    }
    return users;
  }

  /**
   * The simple user.
   */
  @Getter
  @Setter
  @ToString(exclude = "password")
  @EqualsAndHashCode(exclude = "password")
  @NoArgsConstructor
  static class SimpleUser implements Serializable, Principal {

    private static final long serialVersionUID = -1393400622632455935L;

    private String name;

    private String password;

    private List<String> authorities = new ArrayList<>();

    /**
     * Instantiates a new simple user.
     *
     * @param name the name
     * @param password the password
     * @param authorities the authorities
     */
    SimpleUser(String name, String password, String... authorities) {
      this.name = name;
      this.password = password;
      if (authorities != null) {
        Collections.addAll(this.authorities, authorities);
      }
    }
  }

}
