package com.travel.module1_auth.config;

import com.travel.module1_auth.domain.AuthEventType;
import com.travel.module1_auth.service.AuthAuditService;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
public class SessionAuditConfig {

    public static final String ENDED_EXPLICITLY = "travel.sessionEndedExplicitly";

    private final AuthAuditService auditService;

    public SessionAuditConfig(AuthAuditService auditService) {
        this.auditService = auditService;
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @EventListener
    public void onSessionDestroyed(HttpSessionDestroyedEvent event) {
        HttpSession session = event.getSession();
        Object explicit = safeAttribute(session, ENDED_EXPLICITLY);
        if (Boolean.TRUE.equals(explicit)) return;
        Object stored = safeAttribute(session, HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (!(stored instanceof SecurityContext context) || context.getAuthentication() == null) return;

        Authentication auth = context.getAuthentication();
        boolean customer = auth.getAuthorities().stream()
                .anyMatch(a -> CustomerAuthenticationProvider.ROLE_CUSTOMER.equals(a.getAuthority()));
        auditService.record(auth.getName(),
                customer ? AuthEventType.CUSTOMER_SESSION_TERMINATED : AuthEventType.SESSION_EXPIRED,
                null, "Session expired after " + (session.getMaxInactiveInterval() / 60) + " minutes of inactivity");
    }

    public static void markEndedExplicitly(HttpSession session) {
        if (session == null) return;
        try {
            session.setAttribute(ENDED_EXPLICITLY, Boolean.TRUE);
        } catch (IllegalStateException ignored) {

        }
    }

    private static Object safeAttribute(HttpSession session, String name) {
        try {
            return session.getAttribute(name);
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
