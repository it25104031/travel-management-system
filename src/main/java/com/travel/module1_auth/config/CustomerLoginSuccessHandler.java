package com.travel.module1_auth.config;

import com.travel.module1_auth.domain.AuthEventType;
import com.travel.module1_auth.service.AuthAuditService;
import com.travel.module1_auth.service.TempCredentialService;
import com.travel.common.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomerLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthAuditService auditService;
    private final TempCredentialService credentialService;
    private final int customerIdleMinutes;

    public CustomerLoginSuccessHandler(AuthAuditService auditService, TempCredentialService credentialService,
                                       @Value("${app.customer.session-timeout-minutes:15}") int customerIdleMinutes) {
        this.auditService = auditService;
        this.credentialService = credentialService;
        this.customerIdleMinutes = customerIdleMinutes;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        auditService.record(authentication.getName(), AuthEventType.CUSTOMER_LOGIN_SUCCESS, RequestUtils.clientIp(request), null);
        credentialService.markLoggedIn(authentication.getName());

        request.getSession().setMaxInactiveInterval(customerIdleMinutes * 60);
        response.sendRedirect("/portal/case");
    }
}
