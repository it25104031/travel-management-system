package com.travel.module1_auth.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_accounts")
public class CustomerAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, unique = true)
    private String referenceNo;

    @Column(nullable = false)
    private String passkeyHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(unique = true)
    private Long applicationId;

    @Column(name = "customer_email", nullable = false)
    private String email;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() { return expiresAt.isBefore(LocalDateTime.now()); }

    public boolean isUsable() { return Boolean.TRUE.equals(active) && !isExpired(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public String getPasskeyHash() { return passkeyHash; }
    public void setPasskeyHash(String passkeyHash) { this.passkeyHash = passkeyHash; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
