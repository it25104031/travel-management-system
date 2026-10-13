package com.travel.module1_auth.config;

import com.travel.module1_auth.domain.AuthEventType;
import com.travel.module1_auth.service.AdminUserService;
import com.travel.module1_auth.service.AuthAuditService;
import com.travel.module1_auth.service.SiteCustomerService;
import com.travel.module1_auth.service.TempCredentialService;
import com.travel.common.RequestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * Two filter chains:
 *  1. /portal/**  - visa applicants sign in with a reference number + temporary passkey (ROLE_CUSTOMER).
 *  2. everything else - one /login form for
 *       staff     : username + BCrypt password (ROLE_ADMIN / ROLE_USER)  -> staff workspace
 *       travellers: email    + BCrypt password (ROLE_MEMBER)            -> website, cart, profile
 */
@Configuration
public class SecurityConfig {

    private final AdminUserService adminUserService;
    private final TempCredentialService credentialService;
    private final AuthAuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final CustomLoginSuccessHandler staffSuccessHandler;

    public SecurityConfig(AdminUserService adminUserService,
                          TempCredentialService credentialService,
                          AuthAuditService auditService,
                          PasswordEncoder passwordEncoder,
                          CustomLoginSuccessHandler staffSuccessHandler) {
        this.adminUserService = adminUserService;
        this.credentialService = credentialService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.staffSuccessHandler = staffSuccessHandler;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain customerPortalChain(HttpSecurity http,
                                                   CustomerLoginSuccessHandler customerSuccessHandler) throws Exception {
        CustomerAuthenticationProvider provider =
                new CustomerAuthenticationProvider(credentialService, auditService, passwordEncoder);

        http
                .securityMatcher("/portal/**")
                .authenticationManager(new ProviderManager(provider))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/portal/login", "/portal/renew").permitAll()
                        .anyRequest().hasRole("CUSTOMER")
                )
                .formLogin(login -> login
                        .loginPage("/portal/login")
                        .loginProcessingUrl("/portal/login")
                        .usernameParameter("referenceNo")
                        .passwordParameter("passkey")
                        .successHandler(customerSuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            String ref = TempCredentialService.normalise(request.getParameter("referenceNo"));
                            auditService.record(ref, AuthEventType.CUSTOMER_LOGIN_FAILURE,
                                    RequestUtils.clientIp(request), exception.getClass().getSimpleName());
                            String reason = exception instanceof org.springframework.security.authentication.CredentialsExpiredException
                                    ? "expired"
                                    : exception instanceof org.springframework.security.authentication.LockedException ? "locked" : "error";
                            response.sendRedirect("/portal/login?" + reason);
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/portal/logout")
                        .addLogoutHandler((request, response, authentication) -> {
                            SessionAuditConfig.markEndedExplicitly(request.getSession(false));
                            if (authentication != null) {
                                auditService.record(authentication.getName(), AuthEventType.CUSTOMER_SESSION_TERMINATED,
                                        RequestUtils.clientIp(request), "Logged out");
                            }
                        })
                        .logoutSuccessUrl("/portal/login?logout")
                )
                .addFilterAfter(new CustomerSessionValidationFilter(credentialService, auditService), AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain mainChain(HttpSecurity http, SiteCustomerService siteCustomerService) throws Exception {
        // Staff sign in with a username, travellers with their email address - same /login form.
        DaoAuthenticationProvider staffProvider = new DaoAuthenticationProvider(adminUserService);
        staffProvider.setPasswordEncoder(passwordEncoder);
        DaoAuthenticationProvider memberProvider = new DaoAuthenticationProvider(siteCustomerService);
        memberProvider.setPasswordEncoder(passwordEncoder);

        http
                .authenticationManager(new ProviderManager(staffProvider, memberProvider))
                // The REST API (Postman) sends no CSRF token
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC WEBSITE
                        .requestMatchers("/", "/about", "/tours", "/tours/**", "/cart", "/cart/**",
                                "/signup", "/login", "/css/**", "/js/**", "/img/**", "/favicon.ico",
                                "/error", "/error/**").permitAll()

                        // TRAVELLERS (website accounts)
                        .requestMatchers("/my", "/my/**", "/checkout", "/checkout/**").hasRole("MEMBER")

                        // VISA APPLICATIONS: a signed-in traveller, or staff entering one for a walk-in customer
                        .requestMatchers("/visas/apply", "/visas/apply/**", "/visas/select-country")
                        .hasAnyRole("MEMBER", "ADMIN", "USER")

                        // ADMIN-ONLY URLS (users, travellers, countries, visa rules, reports, audit)
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // ALL STAFF (ADMIN + USER)
                        .requestMatchers("/staff", "/staff/**", "/visas/**", "/files/**", "/account/**",
                                "/logistics/**", "/packages/**", "/itineraries/**").hasAnyRole("ADMIN", "USER")

                        // BILLING (Module 3): deleting invoices/payments is admin-only, the rest is for all staff
                        .requestMatchers(HttpMethod.POST, "/billing/invoices/delete/**", "/billing/payments/delete/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/invoices/**", "/api/payments/**",
                                "/api/billing/invoices/**", "/api/billing/payments/**", "/api/packages/**").hasRole("ADMIN")
                        .requestMatchers("/billing/**", "/api/**").hasAnyRole("ADMIN", "USER")

                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .successHandler(staffSuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            String username = request.getParameter("username");
                            auditService.record(username, AuthEventType.LOGIN_FAILURE,
                                    RequestUtils.clientIp(request), exception.getClass().getSimpleName());
                            String reason = exception instanceof org.springframework.security.authentication.LockedException
                                    ? "locked"
                                    : exception instanceof org.springframework.security.authentication.DisabledException ? "disabled" : "error";
                            String staff = "staff".equals(request.getParameter("mode")) ? "&staff" : "";
                            response.sendRedirect("/login?" + reason + staff);
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .addLogoutHandler((request, response, authentication) -> {
                            SessionAuditConfig.markEndedExplicitly(request.getSession(false));
                            if (authentication != null) {
                                auditService.record(authentication.getName(), AuthEventType.LOGOUT,
                                        RequestUtils.clientIp(request), null);
                            }
                        })
                        // Travellers go back to the website, staff to the sign-in page
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.sendRedirect(SiteCustomerService.isMember(authentication) ? "/?signedout" : "/login?logout&staff"))
                        .permitAll()
                );

        return http.build();
    }
}
