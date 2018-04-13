/*
 * Copyright 2017 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * @author Christian Bremer
 */
@Configuration
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfiguration.class);

  private static final String DEFAULT_ACCESS = "hasIpAddress('127.0.0.1') "
      + "or hasIpAddress('::1') or isAuthenticated()";

  private Environment env;

  @Autowired
  public WebSecurityConfiguration(Environment env) {
    this.env = env;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http
        .csrf().disable()
        .httpBasic().realmName(env.getProperty("spring.application.name", "eureka"))
        .and()
        .authorizeRequests()
        .anyRequest()
        .access(env.getProperty("bremersee.access.default-access", DEFAULT_ACCESS));
  }

  @Bean
  @Override
  public UserDetailsService userDetailsService() {

    final String username = env.getProperty("spring.security.user.name", "user");
    final String password = env.getProperty("spring.security.user.password", "changeit");
    final String role = env.getProperty("spring.security.user.roles", "EUREKA");
    LOG.debug("Login user: username = {}, password = {}, roles = {}",
        username, password, role);

    final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    final UserDetails user = User.builder()
        .username(username)
        .password(password)
        .roles(role)
        .passwordEncoder(encoder::encode)
        .build();
    return new InMemoryUserDetailsManager(user);
  }

}
