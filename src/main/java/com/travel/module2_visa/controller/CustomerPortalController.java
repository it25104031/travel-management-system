package com.travel.module2_visa.controller;

import com.travel.module1_auth.domain.CustomerAccountEntity;
import com.travel.module1_auth.service.TempCredentialService;
import com.travel.common.FlashMessages;
import com.travel.module2_visa.domain.ApplicationDocumentEntity;
import com.travel.module2_visa.domain.VisaApplicationEntity;
import com.travel.module2_visa.service.*;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * Applicant self-service (ROLE_CUSTOMER). The application is always resolved from the signed-in
 * reference number, never from an id in the URL, so a customer can only ever see their own case.
 */
@Controller
@RequestMapping("/portal")
public class CustomerPortalController {

    private final TempCredentialService credentialService;
    private final VisaApplicationService applicationService;
    private final ComplianceService complianceService;
    private final ReadinessService readinessService;
    private final PassportValidityService passportService;
    private final DocumentStorageService storage;

    public CustomerPortalController(TempCredentialService credentialService, VisaApplicationService applicationService,
                                    ComplianceService complianceService, ReadinessService readinessService,
                                    PassportValidityService passportService, DocumentStorageService storage) {
        this.credentialService = credentialService;
        this.applicationService = applicationService;
        this.complianceService = complianceService;
        this.readinessService = readinessService;
        this.passportService = passportService;
        this.storage = storage;
    }

    private CustomerAccountEntity account(Authentication auth) {
        return credentialService.findByReference(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("No customer account"));
    }

    private VisaApplicationEntity myApplication(Authentication auth) {
        return applicationService.get(account(auth).getApplicationId());
    }

    @GetMapping({"", "/", "/case"})
    public String myCase(Authentication auth, Model model) {
        CustomerAccountEntity account = account(auth);
        VisaApplicationEntity app = applicationService.get(account.getApplicationId());
        model.addAttribute("account", account);
        model.addAttribute("app", app);
        model.addAttribute("passportMasked", MaskingUtil.mask(app.getPassportNumber()));
        model.addAttribute("slots", applicationService.documentSlots(app));
        model.addAttribute("passport", passportService.check(app));
        model.addAttribute("readiness", readinessService.forTraveller(app));
        return "module2_visa/portal-case";
    }

    @PostMapping("/documents")
    public String upload(@RequestParam String docCode, @RequestParam("file") MultipartFile file,
                         Authentication auth, RedirectAttributes flash) {
        VisaApplicationEntity app = myApplication(auth);
        FlashMessages.run(flash, "Thank you. Your document was uploaded and will be reviewed.",
                () -> applicationService.customerUpload(app.getId(), docCode, file, auth.getName()));
        return "redirect:/portal/case";
    }

    @GetMapping("/documents/{docId}/file")
    public ResponseEntity<Resource> myFile(@PathVariable Long docId, Authentication auth) {
        VisaApplicationEntity app = myApplication(auth);
        ApplicationDocumentEntity doc = applicationService.document(docId);
        if (!doc.getVisaApplication().getId().equals(app.getId())) {
            throw new AccessDeniedException("Not your document");
        }
        return FileController.serve(storage, doc.getFilePath(), doc.getFileType(), doc.getFileName());
    }

    @GetMapping("/approved-visa")
    public ResponseEntity<Resource> approvedVisa(Authentication auth) {
        VisaApplicationEntity app = myApplication(auth);
        return FileController.serve(storage, app.getApprovedVisaPath(), "application/pdf", "visa.pdf");
    }

    @GetMapping("/compliance")
    public String compliance(Authentication auth, Model model) {
        VisaApplicationEntity app = myApplication(auth);
        model.addAttribute("app", app);
        model.addAttribute("record", complianceService.record(app));
        return "module2_visa/portal-compliance";
    }

    @PostMapping("/compliance/insurance")
    public String saveInsurance(@RequestParam String provider, @RequestParam String policyNumber,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate coverFrom,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate coverTo,
                                Authentication auth, RedirectAttributes flash) {
        VisaApplicationEntity app = myApplication(auth);
        FlashMessages.run(flash, "Insurance details saved. Our team will verify them.",
                () -> complianceService.saveInsurance(app.getId(), provider, policyNumber, coverFrom, coverTo));
        return "redirect:/portal/compliance";
    }

    @PostMapping("/compliance/health")
    public String saveHealth(@RequestParam(required = false) Boolean fitToTravel,
                             @RequestParam(required = false) String vaccinationDetails,
                             @RequestParam(required = false) String specialNeedsDetails,
                             @RequestParam String emergencyContactName, @RequestParam String emergencyContactPhone,
                             Authentication auth, RedirectAttributes flash) {
        VisaApplicationEntity app = myApplication(auth);
        FlashMessages.run(flash, "Health declaration saved. Our team will verify it.",
                () -> complianceService.saveHealth(app.getId(), fitToTravel, vaccinationDetails, specialNeedsDetails,
                        emergencyContactName, emergencyContactPhone));
        return "redirect:/portal/compliance";
    }
}
