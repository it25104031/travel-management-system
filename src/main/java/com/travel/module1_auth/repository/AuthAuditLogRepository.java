package com.travel.module1_auth.repository;

import com.travel.module1_auth.domain.AuthAuditLogEntity;
import com.travel.module1_auth.domain.AuthEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLogEntity, Long> {
    long countByUsernameAndEventTypeAndCreatedAtAfter(String username, AuthEventType eventType, LocalDateTime after);

    AuthAuditLogEntity findFirstByUsernameAndEventTypeOrderByCreatedAtDesc(String username, AuthEventType eventType);

    List<AuthAuditLogEntity> findTop300ByOrderByCreatedAtDesc();

    List<AuthAuditLogEntity> findTop300ByUsernameOrderByCreatedAtDesc(String username);
}
