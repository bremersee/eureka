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

package org.bremersee.eureka.config;

import org.bremersee.spring.boot.autoconfigure.security.authentication.AuthenticationProperties;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Web security configuration.
 *
 * @author Christian Bremer
 */
@Configuration
@EnableConfigurationProperties({AuthenticationProperties.class, WebSecurityProperties.class})
@EnableWebSecurity
public class WebSecurityConfiguration {

  private final Environment env;

  private final AuthenticationProperties authenticationProperties;

  private final WebSecurityProperties webSecurityProperties;

  private final CorsConfigurationSource corsConfigurationSource;

  /**
   * Instantiates a new web security configuration.
   *
   * @param env the environment
   * @param authenticationProperties the jbox authentication properties
   * @param webSecurityProperties the security properties
   * @param corsConfigurationSource the cors configuration source
   */
  public WebSecurityConfiguration(
      Environment env,
      AuthenticationProperties authenticationProperties,
      WebSecurityProperties webSecurityProperties,
      CorsConfigurationSource corsConfigurationSource) {
    this.env = env;
    this.authenticationProperties = authenticationProperties;
    this.webSecurityProperties = webSecurityProperties;
    this.corsConfigurationSource = corsConfigurationSource;
  }

  /**
   * Configures the security filter chain.
   *
   * @param http the http security
   * @return the security filter chain
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    String appName = env.getProperty("spring.application.name", "eureka");
    String[] actuatorReadRoles = authenticationProperties.getActuator().getReadRoles()
        .toArray(new String[0]);
    String[] actuatorWriteRoles = authenticationProperties.getActuator().getWriteRoles()
        .toArray(new String[0]);
    return http
        .authorizeHttpRequests(customizer -> customizer
            .requestMatchers(HttpMethod.OPTIONS, "/**")
            .permitAll()

            .requestMatchers(
                EndpointRequest.to(InfoEndpoint.class),
                EndpointRequest.to(HealthEndpoint.class))
            .permitAll()

            .requestMatchers(new AndRequestMatcher(
                EndpointRequest.toAnyEndpoint(),
                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/**")))
            .hasAnyAuthority(actuatorReadRoles)

            .requestMatchers(EndpointRequest.toAnyEndpoint())
            .hasAnyAuthority(actuatorWriteRoles)

            .anyRequest()
            .hasAuthority(webSecurityProperties.getEurekaServerAccessRole()))

        .csrf(AbstractHttpConfigurer::disable)

        .cors(customizer -> customizer
            .configurationSource(corsConfigurationSource))

        .headers(configurer -> configurer
            .frameOptions(FrameOptionsConfig::sameOrigin))

        .sessionManagement(AbstractHttpConfigurer::disable)

        .httpBasic(customizer -> customizer.realmName(appName))

        .formLogin(AbstractHttpConfigurer::disable)

        .build();
  }

}
