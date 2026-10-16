package com.travel.module2_visa.domain;

import com.travel.module1_auth.domain.AdminUserEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tour_incidents")
public class IncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "departure_id", nullable = false)
    private TourDepartureEntity departure;

    private String affectedPassportNumber;

    @Transient
    private String affectedTravellerName;

    @Column(name = "incident_type", nullable = false)
    private String type;

    @Column(nullable = false)
    private String severity;

    private String status = "OPEN";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "action_taken", columnDefinition = "TEXT")
    private String replacementChecklist;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reported_by_staff_id", nullable = false)
    private AdminUserEntity reportedByStaff;
    private String assignedTo;

    @Column(name = "reported_at", updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    public java.util.List<String> getChecklistItems() {
        if (replacementChecklist == null || replacementChecklist.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(replacementChecklist.split("\n")).filter(s -> !s.isBlank()).toList();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TourDepartureEntity getDeparture() { return departure; }
    public void setDeparture(TourDepartureEntity departure) { this.departure = departure; }

    public String getAffectedPassportNumber() { return affectedPassportNumber; }
    public void setAffectedPassportNumber(String affectedPassportNumber) { this.affectedPassportNumber = affectedPassportNumber; }

    public String getAffectedTravellerName() { return affectedTravellerName; }
    public void setAffectedTravellerName(String affectedTravellerName) { this.affectedTravellerName = affectedTravellerName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public String getReplacementChecklist() { return replacementChecklist; }
    public void setReplacementChecklist(String replacementChecklist) { this.replacementChecklist = replacementChecklist; }

    public AdminUserEntity getReportedByStaff() { return reportedByStaff; }
    public void setReportedByStaff(AdminUserEntity reportedByStaff) { this.reportedByStaff = reportedByStaff; }

    public String getReportedBy() { return reportedByStaff == null ? null : reportedByStaff.getUsername(); }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
