package com.travel.module2_visa.domain;

import com.travel.module1_auth.domain.AdminUserEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class VisaApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "visa_application_seq")
    @SequenceGenerator(name = "visa_application_seq", sequenceName = "visa_application_seq", allocationSize = 1)
    private Long id;

    private String givenNames;
    private String surname;
    private String nationality;
    private LocalDate dateOfBirth;
    private String gender;
    @Column(nullable = false, unique = true)
    private String passportNumber;
    private LocalDate passportDateOfIssue;
    private LocalDate passportDateOfExpiry;
    private String portOfArrival;
    private String emailId;

    private Long visaTypeId;

    @Column(name = "gov_portal_ref_no")
    private String govReferenceNo;
    private LocalDate govSubmissionDate;
    @Column(name = "gov_approval_date")
    private LocalDate grantDate;
    @Column(name = "visa_validity_start")
    private LocalDate visaValidFrom;
    @Column(name = "visa_validity_end")
    private LocalDate visaValidTo;
    private String entryType;
    @Column(name = "approved_visa_file_path", length = 500)
    private String approvedVisaPath;

    @ManyToOne
    @JoinColumn(name = "assigned_operator_id")
    private AdminUserEntity assignedOperatorUser;

    @ManyToOne
    @JoinColumn(name = "departure_id")
    private TourDepartureEntity departure;

    private Boolean passportInvalidated = false;

    @Column(name = "site_customer_id")
    private Long siteCustomerId;

    private String status;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ApplicationStatus.PENDING.name();
        }
    }

    public abstract CountryForm getCountryForm();

    public abstract LocalDate getExpectedDateOfArrival();

    public abstract List<? extends ApplicationDocumentEntity> getDocuments();

    public abstract ApplicationDocumentEntity newDocument();

    public String getCountryCode() { return getCountryForm().getCountryCode(); }

    public String getVisaService() { return null; }

    public String getApplicationType() { return getCountryForm().name(); }

    public ApplicationStatus getStatusEnum() { return ApplicationStatus.fromLegacy(status); }

    public String getFullName() {
        return ((givenNames == null ? "" : givenNames) + " " + (surname == null ? "" : surname)).trim();
    }

    public List<ApplicationDocumentEntity> getCurrentDocuments() {
        if (getDocuments() == null) return List.of();
        return getDocuments().stream().filter(d -> d.getStatusEnum() != DocumentStatus.SUPERSEDED)
                .map(d -> (ApplicationDocumentEntity) d).toList();
    }

    public void copyTravelDetailsFrom(VisaApplicationEntity lead) {
        this.portOfArrival = lead.getPortOfArrival();
    }

    public String getAssignedOperator() {
        return assignedOperatorUser == null ? null : assignedOperatorUser.getUsername();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGivenNames() { return givenNames; }
    public void setGivenNames(String givenNames) { this.givenNames = givenNames; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public LocalDate getPassportDateOfIssue() { return passportDateOfIssue; }
    public void setPassportDateOfIssue(LocalDate passportDateOfIssue) { this.passportDateOfIssue = passportDateOfIssue; }

    public LocalDate getPassportDateOfExpiry() { return passportDateOfExpiry; }
    public void setPassportDateOfExpiry(LocalDate passportDateOfExpiry) { this.passportDateOfExpiry = passportDateOfExpiry; }

    public String getPortOfArrival() { return portOfArrival; }
    public void setPortOfArrival(String portOfArrival) { this.portOfArrival = portOfArrival; }

    public Long getSiteCustomerId() { return siteCustomerId; }
    public void setSiteCustomerId(Long siteCustomerId) { this.siteCustomerId = siteCustomerId; }

    public String getEmailId() { return emailId; }
    public void setEmailId(String emailId) { this.emailId = emailId; }

    public Long getVisaTypeId() { return visaTypeId; }
    public void setVisaTypeId(Long visaTypeId) { this.visaTypeId = visaTypeId; }

    public String getGovReferenceNo() { return govReferenceNo; }
    public void setGovReferenceNo(String govReferenceNo) { this.govReferenceNo = govReferenceNo; }

    public LocalDate getGovSubmissionDate() { return govSubmissionDate; }
    public void setGovSubmissionDate(LocalDate govSubmissionDate) { this.govSubmissionDate = govSubmissionDate; }

    public LocalDate getGrantDate() { return grantDate; }
    public void setGrantDate(LocalDate grantDate) { this.grantDate = grantDate; }

    public LocalDate getVisaValidFrom() { return visaValidFrom; }
    public void setVisaValidFrom(LocalDate visaValidFrom) { this.visaValidFrom = visaValidFrom; }

    public LocalDate getVisaValidTo() { return visaValidTo; }
    public void setVisaValidTo(LocalDate visaValidTo) { this.visaValidTo = visaValidTo; }

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public String getApprovedVisaPath() { return approvedVisaPath; }
    public void setApprovedVisaPath(String approvedVisaPath) { this.approvedVisaPath = approvedVisaPath; }

    public AdminUserEntity getAssignedOperatorUser() { return assignedOperatorUser; }
    public void setAssignedOperatorUser(AdminUserEntity assignedOperatorUser) { this.assignedOperatorUser = assignedOperatorUser; }

    public TourDepartureEntity getDeparture() { return departure; }
    public void setDeparture(TourDepartureEntity departure) { this.departure = departure; }

    public Boolean getPassportInvalidated() { return passportInvalidated; }
    public void setPassportInvalidated(Boolean passportInvalidated) { this.passportInvalidated = passportInvalidated; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
