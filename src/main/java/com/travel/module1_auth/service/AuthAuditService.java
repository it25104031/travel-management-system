package com.travel.module1_auth.service;

import com.travel.module1_auth.domain.AuthAuditLogEntity;
import com.travel.module1_auth.domain.AuthEventType;
import com.travel.module1_auth.repository.AuthAuditLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthAuditService {

    private final AuthAuditLogRepository repository;
    private final int maxFailedAttempts;
    private final int lockoutMinutes;

    public AuthAuditService(AuthAuditLogRepository repository,
                            @Value("${app.security.max-failed-attempts:5}") int maxFailedAttempts,
                            @Value("${app.security.lockout-minutes:15}") int lockoutMinutes) {
        this.repository = repository;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutMinutes = lockoutMinutes;
    }

    // Own transaction so an audit row survives even if the caller's transaction rolls back
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String username, AuthEventType type, String ip, String details) {
        String safeName = (username == null || username.isBlank()) ? "(blank)" : username;
        String safeDetails = details != null && details.length() > 500 ? details.substring(0, 500) : details;
        repository.save(new AuthAuditLogEntity(safeName, type, ip, safeDetails));
    }

    /**
     * An account is locked when it has reached the failure limit within the lockout window,
     * counting only failures since its last successful login.
     */
    @Transactional(readOnly = true)
    public boolean isLockedOut(String username, AuthEventType failureType, AuthEventType successType) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(lockoutMinutes);
        AuthAuditLogEntity lastSuccess = repository.findFirstByUsernameAndEventTypeOrderByCreatedAtDesc(username, successType);
        if (lastSuccess != null && lastSuccess.getCreatedAt().isAfter(since)) {
            since = lastSuccess.getCreatedAt();
        }
        return repository.countByUsernameAndEventTypeAndCreatedAtAfter(username, failureType, since) >= maxFailedAttempts;
    }

    @Transactional(readOnly = true)
    public List<AuthAuditLogEntity> recent(String username) {
        if (username != null && !username.isBlank()) {
            return repository.findTop300ByUsernameOrderByCreatedAtDesc(username.trim());
        }
        return repository.findTop300ByOrderByCreatedAtDesc();
    }

    public int getLockoutMinutes() { return lockoutMinutes; }
}
