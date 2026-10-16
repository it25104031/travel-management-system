package com.travel.module2_visa.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visa_status_history")
public class ApplicationStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String applicationType;

    @Column(nullable = false, updatable = false)
    private Long applicationId;

    @Column(name = "previous_status", updatable = false)
    private String fromStatus;

    @Column(name = "new_status", nullable = false, updatable = false)
    private String toStatus;

    @Column(name = "changed_by_user", nullable = false, updatable = false)
    private String changedBy;

    @Column(name = "change_reason", updatable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;

    protected ApplicationStatusHistoryEntity() { }

    public ApplicationStatusHistoryEntity(VisaApplicationEntity application, String fromStatus, String toStatus,
                                          String changedBy, String reason) {
        this.applicationType = application.getApplicationType();
        this.applicationId = application.getId();
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.reason = reason;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getApplicationType() { return applicationType; }
    public Long getApplicationId() { return applicationId; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public String getChangedBy() { return changedBy; }
    public String getReason() { return reason; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
