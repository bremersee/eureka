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

package org.bremersee.eureka.config;

import org.bremersee.security.authentication.AuthenticationProperties;
import org.bremersee.security.authentication.PasswordFlowAuthenticationManager;
import org.bremersee.security.core.AuthorityConstants;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.util.Assert;

/**
 * The security configuration.
 *
 * @author Christian Bremer
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  /**
   * The all basic auth.
   */
  @ConditionalOnProperty(
      prefix = "bremersee.security.authentication",
      name = "enable-jwt-support",
      havingValue = "false", matchIfMissing = true)
  @Order(51)
  @Configuration
  @EnableConfigurationProperties(AuthenticationProperties.class)
  static class AllBasicAuth extends WebSecurityConfigurerAdapter {

    private AuthenticationProperties properties;

    /**
     * Instantiates a new all basic auth.
     *
     * @param properties the properties
     */
    public AllBasicAuth(AuthenticationProperties properties) {
      this.properties = properties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
          .authorizeRequests()
          .antMatchers(HttpMethod.OPTIONS).permitAll()
          .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
          .requestMatchers(EndpointRequest.to(InfoEndpoint.class)).permitAll()
          .requestMatchers(EndpointRequest.toAnyEndpoint())
          .access(properties.getActuator().buildAccessExpression())
          .antMatchers("/eureka/**")
          .access(properties.getEureka().buildAccessExpression())
          .anyRequest()
          .access(properties.getApplication()
              .buildAccessExpression(false, true, false, true, AuthorityConstants.ADMIN_ROLE_NAME))
          .and()
          .userDetailsService(userDetailsService())
          .csrf().disable()
          .formLogin().disable()
          .httpBasic().realmName("eureka");
    }

    @Bean
    @Override
    public UserDetailsService userDetailsService() {
      return new InMemoryUserDetailsManager(properties.getEureka().buildBasicAuthUserDetails(
          properties.buildBasicAuthUserDetails()));
    }
  }

  /**
   * The basic auth.
   */
  @ConditionalOnProperty(
      prefix = "bremersee.security.authentication",
      name = "enable-jwt-support",
      havingValue = "true")
  @Order(51)
  @Configuration
  @EnableConfigurationProperties(AuthenticationProperties.class)
  static class BasicAuth extends WebSecurityConfigurerAdapter {

    private AuthenticationProperties properties;

    /**
     * Instantiates a new application basic auth.
     *
     * @param properties the properties
     */
    public BasicAuth(AuthenticationProperties properties) {
      this.properties = properties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
          .requestMatcher(new AntPathRequestMatcher("/eureka/**"))
          .authorizeRequests()
          .antMatchers(HttpMethod.OPTIONS).permitAll()
          .anyRequest()
          .access(properties.getEureka().buildAccessExpression())
          .and()
          .userDetailsService(userDetailsService())
          .csrf().disable()
          .formLogin().disable()
          .httpBasic().realmName("eureka");
    }

    @Bean
    @Override
    public UserDetailsService userDetailsService() {
      return new InMemoryUserDetailsManager(properties.getEureka().buildBasicAuthUserDetails());
    }
  }

  /**
   * The open id.
   */
  @ConditionalOnProperty(
      prefix = "bremersee.security.authentication",
      name = "enable-jwt-support",
      havingValue = "true")
  @Order(52)
  @Configuration
  @EnableConfigurationProperties(AuthenticationProperties.class)
  static class OpenId extends WebSecurityConfigurerAdapter {

    private final AuthenticationProperties properties;

    private final PasswordFlowAuthenticationManager passwordFlowAuthenticationManager;

    /**
     * Instantiates a new actuator open id.
     *
     * @param properties the properties
     * @param passwordFlowAuthenticationManager the password flow authentication manager
     */
    public OpenId(AuthenticationProperties properties,
        ObjectProvider<PasswordFlowAuthenticationManager> passwordFlowAuthenticationManager) {
      this.properties = properties;
      this.passwordFlowAuthenticationManager = passwordFlowAuthenticationManager.getIfAvailable();
      Assert.notNull(
          this.passwordFlowAuthenticationManager,
          "Password flow authentication manager must be present.");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
          .requestMatcher(new NegatedRequestMatcher(new AntPathRequestMatcher("/eureka/**")))
          .authorizeRequests()
          .antMatchers(HttpMethod.OPTIONS).permitAll()
          .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
          .requestMatchers(EndpointRequest.to(InfoEndpoint.class)).permitAll()
          .requestMatchers(EndpointRequest.toAnyEndpoint())
          .access(properties.getActuator().buildAccessExpression())
          .anyRequest()
          .access(properties.getApplication()
              .buildAccessExpression(false, true, false, true, AuthorityConstants.ADMIN_ROLE_NAME))
          .and()
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
          .and()
          .authenticationProvider(passwordFlowAuthenticationManager)
          .csrf().disable()
          .formLogin().disable()
          .httpBasic()
          .realmName("actuator");
    }
  }

}
