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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(WebSecurityProperties.class)
@Slf4j
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

  private final Environment env;

  private final WebSecurityProperties properties;

  @Autowired
  public WebSecurityConfiguration(Environment env, WebSecurityProperties properties) {
    this.env = env;
    this.properties = properties;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    final String appName = env.getProperty("spring.application.name", "eureka");
    http
        .authorizeRequests()
        .requestMatchers(EndpointRequest.to(InfoEndpoint.class))
        .permitAll()
        .requestMatchers(EndpointRequest.to(HealthEndpoint.class))
        .permitAll()
        .requestMatchers(EndpointRequest.toAnyEndpoint())
        .access(properties.getActuator().buildAccess())
        .antMatchers("/**")
        .access(properties.getApplication().buildAccess())
        .and()
        .csrf().disable()
        .userDetailsService(userDetailsService())
        .httpBasic().realmName(appName);
  }

  /**
   * Creates an in-memory user details service based on the users configured in {@link
   * WebSecurityConfiguration}
   *
   * @return the user details service for the basic authentication
   */
  @Bean
  @Override
  public UserDetailsService userDetailsService() {

    log.info("Building user details service with {}", properties);
    final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    final UserDetails[] userDetails = properties.getUsers().stream().map(
        simpleUser -> User.builder()
            .username(simpleUser.getName())
            .password(simpleUser.getPassword())
            .authorities(
                simpleUser.getAuthorities().toArray(new String[0]))
            .passwordEncoder(encoder::encode)
            .build())
        .toArray(UserDetails[]::new);
    return new InMemoryUserDetailsManager(userDetails);
  }

}
