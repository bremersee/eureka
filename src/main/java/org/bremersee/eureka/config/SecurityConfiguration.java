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
import org.springframework.security.web.util.matcher.AndRequestMatcher;
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
   * The only simple user authentication.
   */
  @ConditionalOnProperty(
      prefix = "bremersee.security.authentication",
      name = "enable-jwt-support",
      havingValue = "false", matchIfMissing = true)
  @Order(51)
  @Configuration
  @EnableConfigurationProperties(AuthenticationProperties.class)
  static class OnlySimpleUserAuthentication extends WebSecurityConfigurerAdapter {

    private AuthenticationProperties properties;

    /**
     * Instantiates a only simple user authentication.
     *
     * @param properties the properties
     */
    public OnlySimpleUserAuthentication(AuthenticationProperties properties) {
      this.properties = properties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
          .authorizeRequests()
          .antMatchers(HttpMethod.OPTIONS).permitAll()
          .antMatchers("/eureka/css/**", "/eureka/js/**", "/eureka/fonts/**",
              "/eureka/images/**").permitAll()
          .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
          .requestMatchers(EndpointRequest.to(InfoEndpoint.class)).permitAll()
          .requestMatchers(new AndRequestMatcher(
              EndpointRequest.toAnyEndpoint(),
              new AntPathRequestMatcher("/**", HttpMethod.GET.name())))
          .access(properties.getActuator().buildAccessExpression(properties::ensureRolePrefix))
          .requestMatchers(EndpointRequest.toAnyEndpoint())
          .access(properties.getActuator().buildAdminAccessExpression(properties::ensureRolePrefix))
          .antMatchers("/eureka/**")
          .access(properties.getEureka().buildAccessExpression(properties::ensureRolePrefix))
          .anyRequest()
          .access(properties.getApplication().buildAccessExpression(
              false, true, false, true,
              properties::ensureRolePrefix,
              AuthorityConstants.ADMIN_ROLE_NAME))
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
   * The eureka authentication.
   */
  @ConditionalOnProperty(
      prefix = "bremersee.security.authentication",
      name = "enable-jwt-support",
      havingValue = "true")
  @Order(51)
  @Configuration
  @EnableConfigurationProperties(AuthenticationProperties.class)
  static class EurekaAuthentication extends WebSecurityConfigurerAdapter {

    private AuthenticationProperties properties;

    /**
     * Instantiates a new eureka authentication.
     *
     * @param properties the properties
     */
    public EurekaAuthentication(AuthenticationProperties properties) {
      this.properties = properties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
          .requestMatcher(new AntPathRequestMatcher("/eureka/**"))
          .authorizeRequests()
          .antMatchers(HttpMethod.OPTIONS).permitAll()
          .antMatchers("/eureka/css/**", "/eureka/js/**", "/eureka/fonts/**",
              "/eureka/images/**").permitAll()
          .anyRequest()
          .access(properties.getEureka().buildAccessExpression(properties::ensureRolePrefix))
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
   * The password flow authentication.
   */
  @ConditionalOnProperty(
      prefix = "bremersee.security.authentication",
      name = "enable-jwt-support",
      havingValue = "true")
  @Order(52)
  @Configuration
  @EnableConfigurationProperties(AuthenticationProperties.class)
  static class PasswordFlowAuthentication extends WebSecurityConfigurerAdapter {

    private final AuthenticationProperties properties;

    private final PasswordFlowAuthenticationManager passwordFlowAuthenticationManager;

    /**
     * Instantiates a new password flow authentication.
     *
     * @param properties the properties
     * @param passwordFlowAuthenticationManager the password flow authentication manager
     */
    public PasswordFlowAuthentication(
        AuthenticationProperties properties,
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
          .requestMatchers(new AndRequestMatcher(
              EndpointRequest.toAnyEndpoint(),
              new AntPathRequestMatcher("/**", HttpMethod.GET.name())))
          .access(properties.getActuator().buildAccessExpression(properties::ensureRolePrefix))
          .requestMatchers(EndpointRequest.toAnyEndpoint())
          .access(properties.getActuator().buildAdminAccessExpression(properties::ensureRolePrefix))
          .anyRequest()
          .access(properties.getApplication().buildAccessExpression(
              false, true, false, true,
              properties::ensureRolePrefix,
              AuthorityConstants.ADMIN_ROLE_NAME))
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
