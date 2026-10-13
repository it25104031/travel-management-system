package com.travel.module2_visa.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A traveller's pre-departure compliance for one tour departure (table traveler_compliance_records):
 * travel insurance plus the health declaration and emergency contact. Keyed by passport number + departure.
 */
@Entity
@Table(name = "traveler_compliance_records")
public class ComplianceRecordEntity {

    public static final String HEALTH_NOT_SUBMITTED = "NOT_SUBMITTED";
    public static final String HEALTH_SUBMITTED = "SUBMITTED";
    public static final String HEALTH_VERIFIED = "VERIFIED";
    public static final String HEALTH_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "traveler_passport_number", nullable = false)
    private String passportNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "departure_id", nullable = false)
    private TourDepartureEntity departure;

    // --- Travel insurance ---
    private String insuranceProvider;
    private String insurancePolicyNumber;
    private LocalDate insuranceStartDate;
    private LocalDate insuranceEndDate;
    @Column(name = "is_insurance_verified")
    private Boolean insuranceVerified = false;
    private String insuranceVerifiedBy;
    private LocalDateTime insuranceVerifiedAt;
    private String insuranceRejectionReason;

    // --- Health declaration and emergency contact ---
    private String healthDeclarationStatus = HEALTH_NOT_SUBMITTED;
    private Boolean fitToTravel;
    @Column(columnDefinition = "TEXT")
    private String vaccinationDetails;
    @Column(columnDefinition = "TEXT")
    private String specialNeedsDetails;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String healthVerifiedBy;
    private LocalDateTime healthVerifiedAt;
    private String healthRejectionReason;

    public boolean isInsuranceSubmitted() {
        return insuranceProvider != null && insurancePolicyNumber != null && insuranceStartDate != null && insuranceEndDate != null;
    }

    /** True when the policy covers every day from departure to return (inclusive). */
    public boolean insuranceCovers(LocalDate start, LocalDate end) {
        if (insuranceStartDate == null || insuranceEndDate == null || start == null || end == null) return false;
        return !insuranceStartDate.isAfter(start) && !insuranceEndDate.isBefore(end);
    }

    public boolean isHealthSubmitted() {
        return healthDeclarationStatus != null && !HEALTH_NOT_SUBMITTED.equals(healthDeclarationStatus);
    }

    public boolean isHealthVerified() { return HEALTH_VERIFIED.equals(healthDeclarationStatus); }

    public boolean isHealthComplete() {
        return fitToTravel != null && notBlank(emergencyContactName) && notBlank(emergencyContactPhone);
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    public Long getId() { return id; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public TourDepartureEntity getDeparture() { return departure; }
    public void setDeparture(TourDepartureEntity departure) { this.departure = departure; }

    public String getInsuranceProvider() { return insuranceProvider; }
    public void setInsuranceProvider(String insuranceProvider) { this.insuranceProvider = insuranceProvider; }

    public String getInsurancePolicyNumber() { return insurancePolicyNumber; }
    public void setInsurancePolicyNumber(String insurancePolicyNumber) { this.insurancePolicyNumber = insurancePolicyNumber; }

    public LocalDate getInsuranceStartDate() { return insuranceStartDate; }
    public void setInsuranceStartDate(LocalDate insuranceStartDate) { this.insuranceStartDate = insuranceStartDate; }

    public LocalDate getInsuranceEndDate() { return insuranceEndDate; }
    public void setInsuranceEndDate(LocalDate insuranceEndDate) { this.insuranceEndDate = insuranceEndDate; }

    public Boolean getInsuranceVerified() { return insuranceVerified; }
    public void setInsuranceVerified(Boolean insuranceVerified) { this.insuranceVerified = insuranceVerified; }

    public String getInsuranceVerifiedBy() { return insuranceVerifiedBy; }
    public void setInsuranceVerifiedBy(String insuranceVerifiedBy) { this.insuranceVerifiedBy = insuranceVerifiedBy; }

    public LocalDateTime getInsuranceVerifiedAt() { return insuranceVerifiedAt; }
    public void setInsuranceVerifiedAt(LocalDateTime insuranceVerifiedAt) { this.insuranceVerifiedAt = insuranceVerifiedAt; }

    public String getInsuranceRejectionReason() { return insuranceRejectionReason; }
    public void setInsuranceRejectionReason(String insuranceRejectionReason) { this.insuranceRejectionReason = insuranceRejectionReason; }

    public String getHealthDeclarationStatus() { return healthDeclarationStatus; }
    public void setHealthDeclarationStatus(String healthDeclarationStatus) { this.healthDeclarationStatus = healthDeclarationStatus; }

    public Boolean getFitToTravel() { return fitToTravel; }
    public void setFitToTravel(Boolean fitToTravel) { this.fitToTravel = fitToTravel; }

    public String getVaccinationDetails() { return vaccinationDetails; }
    public void setVaccinationDetails(String vaccinationDetails) { this.vaccinationDetails = vaccinationDetails; }

    public String getSpecialNeedsDetails() { return specialNeedsDetails; }
    public void setSpecialNeedsDetails(String specialNeedsDetails) { this.specialNeedsDetails = specialNeedsDetails; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public String getHealthVerifiedBy() { return healthVerifiedBy; }
    public void setHealthVerifiedBy(String healthVerifiedBy) { this.healthVerifiedBy = healthVerifiedBy; }

    public LocalDateTime getHealthVerifiedAt() { return healthVerifiedAt; }
    public void setHealthVerifiedAt(LocalDateTime healthVerifiedAt) { this.healthVerifiedAt = healthVerifiedAt; }

    public String getHealthRejectionReason() { return healthRejectionReason; }
    public void setHealthRejectionReason(String healthRejectionReason) { this.healthRejectionReason = healthRejectionReason; }
}
