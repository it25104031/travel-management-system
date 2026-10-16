package com.travel.module2_visa.service;

import com.travel.common.ValidationException;
import com.travel.module2_visa.domain.ComplianceRecordEntity;
import com.travel.module2_visa.domain.VisaApplicationEntity;
import com.travel.module2_visa.repository.ComplianceRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ComplianceService {

    public static final String NO_DEPARTURE = "Insurance and health details can be added once you are part of a tour departure.";

    private final ComplianceRecordRepository recordRepository;
    private final VisaApplicationService applicationService;

    public ComplianceService(ComplianceRecordRepository recordRepository, VisaApplicationService applicationService) {
        this.recordRepository = recordRepository;
        this.applicationService = applicationService;
    }

    @Transactional(readOnly = true)
    public ComplianceRecordEntity record(VisaApplicationEntity app) {
        if (app.getDeparture() == null || app.getPassportNumber() == null) return null;
        return recordRepository.findFirstByPassportNumberAndDeparture(app.getPassportNumber(), app.getDeparture()).orElse(null);
    }

    private ComplianceRecordEntity recordForUpdate(VisaApplicationEntity app) {
        if (app.getDeparture() == null) throw new ValidationException(NO_DEPARTURE);
        return recordRepository.findFirstByPassportNumberAndDeparture(app.getPassportNumber(), app.getDeparture())
                .orElseGet(() -> {
                    ComplianceRecordEntity r = new ComplianceRecordEntity();
                    r.setPassportNumber(app.getPassportNumber());
                    r.setDeparture(app.getDeparture());
                    return r;
                });
    }

    @Transactional
    public void saveInsurance(Long applicationId, String provider, String policyNumber, LocalDate coverFrom, LocalDate coverTo) {
        List<String> errors = new ArrayList<>();
        if (provider == null || provider.isBlank()) errors.add("Insurance provider is required.");
        if (policyNumber == null || policyNumber.isBlank()) errors.add("Policy number is required.");
        if (coverFrom == null || coverTo == null) errors.add("Coverage dates are required.");
        else if (coverTo.isBefore(coverFrom)) errors.add("Coverage end date must be after the start date.");
        if (!errors.isEmpty()) throw new ValidationException(errors);

        ComplianceRecordEntity record = recordForUpdate(applicationService.get(applicationId));
        record.setInsuranceProvider(provider.trim());
        record.setInsurancePolicyNumber(policyNumber.trim());
        record.setInsuranceStartDate(coverFrom);
        record.setInsuranceEndDate(coverTo);
        record.setInsuranceVerified(false);
        record.setInsuranceVerifiedBy(null);
        record.setInsuranceVerifiedAt(null);
        record.setInsuranceRejectionReason(null);
        recordRepository.save(record);
    }

    @Transactional
    public void saveHealth(Long applicationId, Boolean fitToTravel, String vaccinationDetails, String specialNeeds,
                           String contactName, String contactPhone) {
        List<String> errors = new ArrayList<>();
        if (fitToTravel == null) errors.add("Please answer whether you are fit to travel.");
        if (contactName == null || contactName.isBlank()) errors.add("Emergency contact name is required.");
        if (contactPhone == null || contactPhone.isBlank()) errors.add("Emergency contact phone is required.");
        if (!errors.isEmpty()) throw new ValidationException(errors);

        ComplianceRecordEntity record = recordForUpdate(applicationService.get(applicationId));
        record.setFitToTravel(fitToTravel);
        record.setVaccinationDetails(blankToNull(vaccinationDetails));
        record.setSpecialNeedsDetails(blankToNull(specialNeeds));
        record.setEmergencyContactName(contactName.trim());
        record.setEmergencyContactPhone(contactPhone.trim());
        record.setHealthDeclarationStatus(ComplianceRecordEntity.HEALTH_SUBMITTED);
        record.setHealthVerifiedBy(null);
        record.setHealthVerifiedAt(null);
        record.setHealthRejectionReason(null);
        recordRepository.save(record);
    }

    @Transactional
    public void reviewInsurance(Long applicationId, boolean approve, String reason, String actor) {
        VisaApplicationEntity app = applicationService.get(applicationId);
        ComplianceRecordEntity record = record(app);
        if (record == null || !record.isInsuranceSubmitted()) {
            throw new ValidationException("No insurance details have been submitted.");
        }
        if (approve) {
            LocalDate start = app.getDeparture().getDepartureDate();
            LocalDate end = PassportValidityService.travelEnd(app);
            if (!record.insuranceCovers(start, end)) {
                throw new ValidationException("The policy does not cover the whole trip (" + start + " to " + end + ").");
            }
            record.setInsuranceVerified(true);
            record.setInsuranceRejectionReason(null);
        } else {
            if (reason == null || reason.isBlank()) throw new ValidationException("A reason is required.");
            record.setInsuranceVerified(false);
            record.setInsuranceRejectionReason(reason.trim());
            applicationService.notifyCustomer(app, "Please update your travel insurance",
                    "Your insurance details need attention: " + reason.trim());
        }
        record.setInsuranceVerifiedBy(actor);
        record.setInsuranceVerifiedAt(LocalDateTime.now());
    }

    @Transactional
    public void reviewHealth(Long applicationId, boolean approve, String reason, String actor) {
        VisaApplicationEntity app = applicationService.get(applicationId);
        ComplianceRecordEntity record = record(app);
        if (record == null || !record.isHealthSubmitted()) {
            throw new ValidationException("No health declaration has been submitted.");
        }
        if (approve) {
            if (!record.isHealthComplete()) throw new ValidationException("The health declaration is incomplete.");
            record.setHealthDeclarationStatus(ComplianceRecordEntity.HEALTH_VERIFIED);
            record.setHealthRejectionReason(null);
        } else {
            if (reason == null || reason.isBlank()) throw new ValidationException("A reason is required.");
            record.setHealthDeclarationStatus(ComplianceRecordEntity.HEALTH_REJECTED);
            record.setHealthRejectionReason(reason.trim());
            applicationService.notifyCustomer(app, "Please update your health declaration",
                    "Your health declaration needs attention: " + reason.trim());
        }
        record.setHealthVerifiedBy(actor);
        record.setHealthVerifiedAt(LocalDateTime.now());
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
