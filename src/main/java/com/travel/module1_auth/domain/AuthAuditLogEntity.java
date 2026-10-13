package com.travel.module1_auth.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

/** Append-only record of authentication and account events. No setters after creation. */
@Entity
@Table(name = "auth_audit_log")
public class AuthAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String username;

    // Stored as plain text (not a MySQL ENUM) so new event types never need a schema change
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, updatable = false, length = 40)
    private AuthEventType eventType;

    @Column(updatable = false, length = 64)
    private String ipAddress;

    @Column(updatable = false, length = 500)
    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AuthAuditLogEntity() { }

    public AuthAuditLogEntity(String username, AuthEventType eventType, String ipAddress, String details) {
        this.username = username;
        this.eventType = eventType;
        this.ipAddress = ipAddress;
        this.details = details;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public AuthEventType getEventType() { return eventType; }
    public String getIpAddress() { return ipAddress; }
    public String getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
