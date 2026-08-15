package com.dentalclinic.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public FilterRegistrationBean<LoginThrottleFilter> loginThrottleFilterRegistration(
            LoginThrottleFilter loginThrottleFilter) {
        FilterRegistrationBean<LoginThrottleFilter> registration = new FilterRegistrationBean<>(loginThrottleFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   LoginThrottleFilter loginThrottleFilter,
                                                   ClinicAuthenticationSuccessHandler successHandler,
                                                   ClinicAuthenticationFailureHandler failureHandler,
                                                   ClinicLogoutSuccessHandler logoutSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/randevu-al", "/randevu-al/**",
                                "/css/**", "/js/**", "/webjars/**", "/error",
                                "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/appointment-requests/**", "/working-hours/**")
                            .hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers("/patients/new", "/patients/*/edit")
                            .hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.POST, "/patients/**")
                            .hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers("/appointments/new", "/appointments/*/edit")
                            .hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.POST, "/appointments", "/appointments/*/edit")
                            .hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.POST, "/appointments/*/status")
                            .hasAnyRole("ADMIN", "RECEPTIONIST", "DENTIST")
                        .requestMatchers("/appointments/*/visit-note")
                            .hasAnyRole("ADMIN", "DENTIST")
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.migrateSession())
                        .maximumSessions(1))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; object-src 'none'; base-uri 'self'; " +
                                "frame-ancestors 'none'; form-action 'self'; img-src 'self' data:; " +
                                "style-src 'self'; script-src 'self'; font-src 'self'"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                "camera=(), microphone=(), geolocation=(), payment=(), usb=()")))
                .addFilterBefore(loginThrottleFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
