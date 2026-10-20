package com.travel.module1_auth.config;

import com.travel.module1_auth.domain.AuthEventType;
import com.travel.module1_auth.domain.CustomerAccountEntity;
import com.travel.module1_auth.service.AuthAuditService;
import com.travel.module1_auth.service.TempCredentialService;
import com.travel.common.RequestUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

public class CustomerSessionValidationFilter extends OncePerRequestFilter {

    private final TempCredentialService credentialService;
    private final AuthAuditService auditService;

    public CustomerSessionValidationFilter(TempCredentialService credentialService, AuthAuditService auditService) {
        this.credentialService = credentialService;
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isCustomer = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> CustomerAuthenticationProvider.ROLE_CUSTOMER.equals(a.getAuthority()));

        if (isCustomer) {
            Optional<CustomerAccountEntity> account = credentialService.findByReference(auth.getName());
            if (account.isEmpty() || !account.get().isUsable()) {
                boolean expired = account.isPresent() && account.get().isExpired();
                auditService.record(auth.getName(), AuthEventType.CUSTOMER_SESSION_TERMINATED,
                        RequestUtils.clientIp(request), expired ? "Session ended: passkey expired" : "Session ended: account closed");
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                SessionAuditConfig.markEndedExplicitly(session);
                if (session != null) session.invalidate();
                response.sendRedirect("/portal/login?" + (expired ? "expired" : "closed"));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
