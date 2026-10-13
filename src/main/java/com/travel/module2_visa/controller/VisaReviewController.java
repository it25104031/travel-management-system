package com.travel.module2_visa.controller;

import com.travel.module1_auth.domain.AuthEventType;
import com.travel.module1_auth.service.AdminUserService;
import com.travel.module1_auth.service.AuthAuditService;
import com.travel.module1_auth.service.TempCredentialService;
import com.travel.common.FlashMessages;
import com.travel.common.GlobalModelAdvice;
import com.travel.common.RequestUtils;
import com.travel.module2_visa.domain.ApplicationStatus;
import com.travel.module2_visa.domain.EntryType;
import com.travel.module2_visa.domain.VisaApplicationEntity;
import com.travel.module2_visa.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** Staff (ADMIN + USER) review workspace for visa applications. */
@Controller
@RequestMapping("/visas/dashboard")
public class VisaReviewController {

    private final VisaApplicationService applicationService;
    private final VisaQueueService queueService;
    private final ComplianceService complianceService;
    private final ReadinessService readinessService;
    private final VisaRuleService ruleService;
    private final TempCredentialService credentialService;
    private final AdminUserService userService;
    private final AuthAuditService auditService;
    private final ConversionService conversionService;

    public VisaReviewController(VisaApplicationService applicationService, VisaQueueService queueService,
                                ComplianceService complianceService, ReadinessService readinessService,
                                VisaRuleService ruleService, TempCredentialService credentialService,
                                AdminUserService userService, AuthAuditService auditService,
                                @Qualifier("mvcConversionService") ConversionService conversionService) {
        this.conversionService = conversionService;
        this.applicationService = applicationService;
        this.queueService = queueService;
        this.complianceService = complianceService;
        this.readinessService = readinessService;
        this.ruleService = ruleService;
        this.credentialService = credentialService;
        this.userService = userService;
        this.auditService = auditService;
    }

    // The only applicant fields staff may change from the review page (others are read-only there)
    private static final String[] EDITABLE_FIELDS = {"givenNames", "surname", "passportNumber", "emailId", "passportDateOfExpiry",
            "expectedDateOfArrival", "presentHouseNo", "presentVillageTownCity", "presentCountry", "presentMobileNo",
            "intendedArrivalDate", "contactNumber"};

    // 4. Visa Dashboard (work queue)
    @GetMapping
    public String showDashboard(@RequestParam(defaultValue = "open") String filter,
                                @RequestParam(required = false) String status,
                                Model model, Authentication authentication) {
        boolean isAdmin = GlobalModelAdvice.hasRole("ROLE_ADMIN");
        model.addAttribute("rows", queueService.queue(filter, status, authentication.getName(), !isAdmin));
        model.addAttribute("filter", filter);
        model.addAttribute("status", status);
        model.addAttribute("statuses", ApplicationStatus.values());
        return "module2_visa/visa-dashboard";
    }

    // 5. Review Application Details
    @GetMapping("/{id}")
    public String viewApplicationDetails(@PathVariable Long id, @RequestParam(required = false) boolean reveal,
                                         Model model, Authentication auth, HttpServletRequest request) {
        VisaApplicationEntity app = applicationService.get(id);
        boolean isAdmin = GlobalModelAdvice.hasRole("ROLE_ADMIN");
        boolean showFull = isAdmin || reveal;
        if (reveal && !isAdmin) {
            // Operators may reveal the full number when they need to type it into the government portal; it is audited
            auditService.record(auth.getName(), AuthEventType.SENSITIVE_DATA_REVEALED, RequestUtils.clientIp(request),
                    "Passport number of application " + id);
        }
        model.addAttribute("app", app);
        model.addAttribute("passportDisplay", showFull ? app.getPassportNumber() : MaskingUtil.mask(app.getPassportNumber()));
        model.addAttribute("passportRevealed", showFull);
        model.addAttribute("row", queueService.toRow(app, !showFull));
        model.addAttribute("slots", applicationService.documentSlots(app));
        model.addAttribute("history", applicationService.history(id));
        model.addAttribute("record", complianceService.record(app));
        model.addAttribute("readiness", readinessService.forTraveller(app));
        model.addAttribute("visaType", ruleService.visaType(app.getVisaTypeId()).orElse(null));
        model.addAttribute("customerAccount", credentialService.findByApplication(id).orElse(null));
        model.addAttribute("statuses", ApplicationStatus.values());
        model.addAttribute("entryTypes", EntryType.values());
        model.addAttribute("operators", userService.findAll().stream().filter(u -> u.isActiveAccount()).toList());
        return "module2_visa/visa-details";
    }

    // 6. Update editable applicant details
    @PostMapping("/update")
    public String updateApplication(@RequestParam Long id, HttpServletRequest request,
                                    Authentication auth, RedirectAttributes flash) {
        // Bind onto the same entity type as the stored application (India / Sri Lanka)
        VisaApplicationEntity updatedApp = applicationService.get(id).getCountryForm().newApplication();
        ServletRequestDataBinder binder = new ServletRequestDataBinder(updatedApp, "app");
        binder.setConversionService(conversionService);
        binder.setAllowedFields(EDITABLE_FIELDS);
        binder.bind(request);
        updatedApp.setId(id);
        FlashMessages.run(flash, "Applicant details saved.", () -> applicationService.updateDetails(updatedApp, auth.getName()));
        return "redirect:/visas/dashboard/" + updatedApp.getId();
    }

    @PostMapping("/{id}/documents")
    public String uploadDocument(@PathVariable Long id, @RequestParam(required = false) String docCode,
                                 @RequestParam("file") MultipartFile file, Authentication auth, RedirectAttributes flash) {
        FlashMessages.run(flash, "Document uploaded.", () -> applicationService.uploadDocument(id, docCode, file, auth.getName()));
        return "redirect:/visas/dashboard/" + id + "#documents";
    }

    @PostMapping("/documents/{docId}/verify")
    public String verifyDocument(@PathVariable Long docId, @RequestParam Long applicationId, Authentication auth, RedirectAttributes flash) {
        FlashMessages.run(flash, "Document marked VERIFIED.", () -> applicationService.verifyDocument(docId, auth.getName()));
        return "redirect:/visas/dashboard/" + applicationId + "#documents";
    }

    @PostMapping("/documents/{docId}/reject")
    public String rejectDocument(@PathVariable Long docId, @RequestParam Long applicationId, @RequestParam String reason,
                                 Authentication auth, RedirectAttributes flash) {
        FlashMessages.run(flash, "Document REJECTED; the applicant has been asked to re-upload.",
                () -> applicationService.rejectDocument(docId, reason, auth.getName()));
        return "redirect:/visas/dashboard/" + applicationId + "#documents";
    }

    @PostMapping("/documents/{docId}/delete")
    public String deleteDocument(@PathVariable Long docId, @RequestParam Long applicationId, RedirectAttributes flash) {
        FlashMessages.run(flash, "Document deleted from the database and disk.", () -> applicationService.deleteDocument(docId));
        return "redirect:/visas/dashboard/" + applicationId + "#documents";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam ApplicationStatus status,
                               @RequestParam(required = false) String reason, Authentication auth, RedirectAttributes flash) {
        FlashMessages.run(flash, "Status changed to " + status.getLabel() + ".",
                () -> applicationService.changeStatus(id, status, reason, auth.getName()));
        return "redirect:/visas/dashboard/" + id;
    }

    @PostMapping("/{id}/gov-reference")
    public String govReference(@PathVariable Long id, @RequestParam String govReferenceNo, RedirectAttributes flash) {
        FlashMessages.run(flash, "Government reference saved.", () -> applicationService.recordGovReference(id, govReferenceNo));
        return "redirect:/visas/dashboard/" + id + "#government";
    }

    @PostMapping("/{id}/grant")
    public String grant(@PathVariable Long id,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate grantDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validTo,
                        @RequestParam(required = false) String entryType,
                        @RequestParam(value = "approvedVisa", required = false) MultipartFile approvedVisa,
                        Authentication auth, RedirectAttributes flash) {
        FlashMessages.run(flash, "Visa grant recorded and application APPROVED.",
                () -> applicationService.recordGrant(id, grantDate, validFrom, validTo, entryType, approvedVisa, auth.getName()));
        return "redirect:/visas/dashboard/" + id + "#government";
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam(required = false) String operator, RedirectAttributes flash) {
        if (!GlobalModelAdvice.hasRole("ROLE_ADMIN")) {
            flash.addFlashAttribute("errors", java.util.List.of("Only administrators can assign applications."));
        } else {
            FlashMessages.run(flash, "Assignment updated.", () -> applicationService.assign(id, operator));
        }
        return "redirect:/visas/dashboard/" + id;
    }

    @PostMapping("/{id}/insurance/review")
    public String reviewInsurance(@PathVariable Long id, @RequestParam boolean approve, @RequestParam(required = false) String reason,
                                  Authentication auth, RedirectAttributes flash) {
        FlashMessages.run(flash, approve ? "Insurance verified." : "Insurance returned to the applicant.",
                () -> complianceService.reviewInsurance(id, approve, reason, auth.getName()));
        return "redirect:/visas/dashboard/" + id + "#compliance";
    }

    @PostMapping("/{id}/health/review")
    public String reviewHealth(@PathVariable Long id, @RequestParam boolean approve, @RequestParam(required = false) String reason,
                               Authentication auth, RedirectAttributes flash) {
        FlashMessages.run(flash, approve ? "Health declaration verified." : "Health declaration returned to the applicant.",
                () -> complianceService.reviewHealth(id, approve, reason, auth.getName()));
        return "redirect:/visas/dashboard/" + id + "#compliance";
    }

    @PostMapping("/{id}/resend-credentials")
    public String resendCredentials(@PathVariable Long id, RedirectAttributes flash) {
        VisaApplicationEntity app = applicationService.get(id);
        TempCredentialService.IssuedCredentials creds = credentialService.issue(id, app.getCountryCode(), app.getEmailId());
        flash.addFlashAttribute("success", "New passkey issued for " + creds.referenceNo()
                + (creds.emailed() ? " and emailed to the applicant." : ". Email is not configured, so give the applicant this passkey: " + creds.passkey()));
        return "redirect:/visas/dashboard/" + id;
    }
}
