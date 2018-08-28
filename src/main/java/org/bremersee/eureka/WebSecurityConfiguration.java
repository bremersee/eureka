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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

/**
 * @author Christian Bremer
 */
@Configuration
@EnableConfigurationProperties(WebSecurityProperties.class)
@Slf4j
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfiguration.class);

  private static final String DEFAULT_ACCESS = "hasIpAddress('127.0.0.1') "
      + "or hasIpAddress('::1') or isAuthenticated()";

  private Environment env;

  private final WebSecurityProperties properties;

  @Autowired
  public WebSecurityConfiguration(Environment env, WebSecurityProperties properties) {
    this.env = env;
    this.properties = properties;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    final String appName = env.getProperty("spring.application.name", "eureka");
    /*
    http
        .csrf().disable()
        .httpBasic().realmName(env.getProperty("spring.application.name", "eureka"))
        .and()
        .authorizeRequests()
        .anyRequest()
        .access(env.getProperty("bremersee.access.default-access", DEFAULT_ACCESS));
    */
    /*
    http
        .requestMatcher(EndpointRequest.toAnyEndpoint())
          .authorizeRequests()
            .antMatchers(HttpMethod.GET, "/actuator/health").permitAll()
            .antMatchers(HttpMethod.GET, "/actuator/info").permitAll()
            .anyRequest()
            .access(properties.getActuator().buildAccess())
        .and()
          .requestMatcher(new NegatedRequestMatcher(EndpointRequest.toAnyEndpoint()))
          .authorizeRequests()
          .anyRequest()
          .access(properties.getApplication().buildAccess())
        .and()
          .csrf().disable()
          .userDetailsService(userDetailsService())
          .httpBasic().realmName(appName);
    */

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

  /*
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
