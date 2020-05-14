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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bremersee.actuator.security.authentication.ActuatorAuthProperties;
import org.bremersee.security.authentication.AuthProperties;
import org.bremersee.security.authentication.AuthProperties.PathMatcherProperties;
import org.bremersee.security.authentication.PasswordFlowAuthenticationManager;
import org.bremersee.security.core.AuthorityConstants;
import org.bremersee.web.CorsProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

/**
 * The security configuration.
 *
 * @author Christian Bremer
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  /**
   * The eureka security configuration.
   */
  @Configuration
  @EnableConfigurationProperties({
      AuthProperties.class,
      CorsProperties.class
  })
  @Order(49)
  static class Eureka extends WebSecurityConfigurerAdapter {

    private final AuthProperties authProperties;

    private final CorsProperties corsProperties;

    private final PasswordEncoder passwordEncoder;

    /**
     * Instantiates a new eureka security configuration.
     *
     * @param authProperties the auth properties
     * @param corsProperties the cors properties
     * @param passwordEncoder the password encoder
     */
    public Eureka(
        AuthProperties authProperties,
        CorsProperties corsProperties,
        PasswordEncoder passwordEncoder) {
      this.authProperties = authProperties;
      this.corsProperties = corsProperties;
      this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void configure(WebSecurity web) {
      web
          .ignoring()
          .antMatchers(HttpMethod.OPTIONS)
          .antMatchers("/eureka/css/**", "/eureka/js/**", "/eureka/fonts/**",
              "/eureka/images/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
          .requestMatcher(new AntPathRequestMatcher("/eureka/**"))
          .authorizeRequests()
          .anyRequest()
          .access(
              authProperties.getEureka().buildAccessExpression(authProperties::ensureRolePrefix))
          .and()
          .cors(customizer -> {
            if (!corsProperties.isEnable()) {
              customizer.disable();
            }
          })
          .csrf().disable()
          .formLogin().disable()
          .httpBasic().realmName("eureka");
    }

    @Bean
    @Override
    public UserDetailsService userDetailsServiceBean() {
      UserDetails[] other = authProperties.buildBasicAuthUserDetails(passwordEncoder);
      UserDetails[] all = authProperties.getEureka()
          .buildBasicAuthUserDetails(passwordEncoder, other);
      return new InMemoryUserDetailsManager(all);
    }

  }

  /**
   * The app and actuator security configuration.
   */
  @Configuration
  @EnableConfigurationProperties({
      AuthProperties.class,
      ActuatorAuthProperties.class
  })
  @Order(50)
  static class AppAndActuator extends WebSecurityConfigurerAdapter {

    private final AuthProperties authProperties;

    private final ActuatorAuthProperties actuatorAuthProperties;

    private final PasswordFlowAuthenticationManager authenticationManager;

    /**
     * Instantiates a new app and actuator security configuration.
     *
     * @param authProperties the auth properties
     * @param actuatorAuthProperties the actuator auth properties
     * @param authenticationManagerProvider the authentication manager provider
     */
    public AppAndActuator(
        AuthProperties authProperties,
        ActuatorAuthProperties actuatorAuthProperties,
        ObjectProvider<PasswordFlowAuthenticationManager> authenticationManagerProvider) {
      this.authProperties = authProperties;
      this.actuatorAuthProperties = actuatorAuthProperties;
      this.authenticationManager = authenticationManagerProvider.getIfAvailable();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

      List<String> roles = new ArrayList<>(authProperties.getRoleDefinitions()
          .getOrDefault("admin", Collections.emptyList()));
      if (roles.isEmpty()) {
        roles.add(AuthorityConstants.ADMIN_ROLE_NAME);
      }
      PathMatcherProperties anyRequestMatcher = new PathMatcherProperties();
      anyRequestMatcher.setRoles(roles);

      if (authenticationManager != null) {
        http.authenticationProvider(authenticationManager);
      }

      http
          .requestMatcher(new NegatedRequestMatcher(new AntPathRequestMatcher("/eureka/**")))
          .authorizeRequests()
          .requestMatchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
          .requestMatchers(EndpointRequest.to(InfoEndpoint.class)).permitAll()
          .requestMatchers(new AndRequestMatcher(
              EndpointRequest.toAnyEndpoint(),
              new AntPathRequestMatcher("/**", HttpMethod.GET.name())))
          .access(actuatorAuthProperties.buildAccessExpression())
          .requestMatchers(EndpointRequest.toAnyEndpoint())
          .access(actuatorAuthProperties.buildAdminAccessExpression())
          .anyRequest()
          .access(anyRequestMatcher.accessExpression(authProperties::ensureRolePrefix))
          .and()
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
          .and()
          .cors(customizer -> {
            if (!actuatorAuthProperties.isEnableCors()) {
              customizer.disable();
            }
          })
          .csrf().disable()
          .formLogin().disable()
          .httpBasic()
          .realmName("eureka");
    }

  }

}
