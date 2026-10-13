package com.travel.module2_visa.service;

import com.travel.module1_auth.service.TempCredentialService;
import com.travel.module2_visa.domain.*;
import com.travel.module2_visa.repository.VisaApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Daily background jobs: expiry warnings, re-upload reminders and closing expired customer access. */
@Component
public class ComplianceScheduler {

    private static final Logger log = LoggerFactory.getLogger(ComplianceScheduler.class);

    private final VisaApplicationRepository applicationRepository;
    private final VisaApplicationService applicationService;
    private final PassportValidityService passportService;
    private final ComplianceService complianceService;
    private final TempCredentialService credentialService;

    public ComplianceScheduler(VisaApplicationRepository applicationRepository, VisaApplicationService applicationService,
                               PassportValidityService passportService, ComplianceService complianceService,
                               TempCredentialService credentialService) {
        this.applicationRepository = applicationRepository;
        this.applicationService = applicationService;
        this.passportService = passportService;
        this.complianceService = complianceService;
        this.credentialService = credentialService;
    }

    @Scheduled(cron = "${app.scheduler.daily-cron:0 0 7 * * *}")
    @Transactional
    public void runDailyChecks() {
        int warnings = 0, reminders = 0;
        List<VisaApplicationEntity> apps = applicationRepository.findAll();
        for (VisaApplicationEntity app : apps) {
            if (app.getStatusEnum() == ApplicationStatus.REJECTED) continue;
            if (app.getDeparture() != null && app.getDeparture().getStatusEnum() == DepartureStatus.DEPARTED) continue;
            LocalDate travel = PassportValidityService.travelEnd(app);
            if (travel != null && travel.isBefore(LocalDate.now())) continue;

            PassportValidityService.PassportCheck passport = passportService.check(app);
            if (passport.result() == PassportValidityService.Result.EXPIRES_WITHIN_6_MONTHS
                    || passport.result() == PassportValidityService.Result.EXPIRED) {
                applicationService.notifyCustomer(app, "Passport validity warning",
                        "Your passport expires on " + app.getPassportDateOfExpiry() + ". Most destinations require it to be valid "
                                + "until at least " + passport.requiredUntil() + ". Please renew it and upload the new bio page.");
                warnings++;
            }

            boolean hasRejected = app.getCurrentDocuments().stream()
                    .anyMatch(d -> d.getStatusEnum() == DocumentStatus.REJECTED || d.getStatusEnum() == DocumentStatus.INVALIDATED);
            if (hasRejected) {
                applicationService.notifyCustomer(app, "Reminder: documents needed",
                        "One or more of your documents still need to be replaced. Please sign in to the applicant portal.");
                reminders++;
            }

            ComplianceRecordEntity record = complianceService.record(app);
            LocalDate coverEnd = record == null ? null : record.getInsuranceEndDate();
            if (app.getStatusEnum() == ApplicationStatus.APPROVED && coverEnd != null
                    && travel != null && coverEnd.isBefore(travel)) {
                applicationService.notifyCustomer(app, "Travel insurance warning",
                        "Your travel insurance ends on " + coverEnd + ", before your trip ends on " + travel + ".");
                warnings++;
            }
        }
        int closed = credentialService.deactivateExpired();
        log.info("Daily compliance checks: {} expiry warnings, {} re-upload reminders, {} expired customer accounts closed",
                warnings, reminders, closed);
    }
}
