package com.travel.module2_visa.service;

import com.travel.module1_auth.domain.AdminUserEntity;
import com.travel.module1_auth.repository.AdminUserRepository;
import com.travel.module1_auth.service.TempCredentialService;
import com.travel.module1_auth.service.TempCredentialService.IssuedCredentials;
import com.travel.common.NotificationService;
import com.travel.common.ValidationException;
import com.travel.module2_visa.domain.*;
import com.travel.module2_visa.repository.ApplicationStatusHistoryRepository;
import com.travel.module2_visa.repository.ApplicationDocumentRepository;
import com.travel.module2_visa.repository.VisaApplicationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * All application writes go through here so that each multi-step operation (form + files,
 * status + history + notification) commits or rolls back as one unit.
 */
@Service
public class VisaApplicationService {

    public static final String OTHER_DOC_CODE = "OTHER";
    public static final String PASSPORT_DOC_CODE = "PASSPORT_BIO";

    /** One required upload slot together with the file currently filling it (if any). */
    public record DocumentSlot(RequiredDocumentEntity rule, ApplicationDocumentEntity current) {
        public boolean isMandatory() { return rule == null || Boolean.TRUE.equals(rule.getMandatory()); }
        public boolean isSatisfied() { return current != null && current.getStatusEnum() == DocumentStatus.VERIFIED; }
        public boolean needsUpload() {
            return current == null || current.getStatusEnum() == DocumentStatus.REJECTED
                    || current.getStatusEnum() == DocumentStatus.INVALIDATED;
        }
        public String getLabel() { return rule != null ? rule.getLabel() : current.getDocCode(); }
        public String getDocCode() { return rule != null ? rule.getDocCode() : current.getDocCode(); }
    }

    public record SubmissionResult(VisaApplicationEntity application, IssuedCredentials credentials) { }

    private final VisaApplicationRepository repository;
    private final ApplicationDocumentRepository documentRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final DocumentStorageService storage;
    private final VisaRuleService ruleService;
    private final TempCredentialService credentialService;
    private final NotificationService notificationService;
    private final String portalUrl;
    private final AdminUserRepository adminUserRepository;

    public VisaApplicationService(VisaApplicationRepository repository,
                                  ApplicationDocumentRepository documentRepository,
                                  ApplicationStatusHistoryRepository historyRepository,
                                  DocumentStorageService storage,
                                  VisaRuleService ruleService,
                                  TempCredentialService credentialService,
                                  NotificationService notificationService,
                                  AdminUserRepository adminUserRepository,
                                  @Value("${app.customer.portal-url:http://localhost:8080/portal/login}") String portalUrl) {
        this.repository = repository;
        this.documentRepository = documentRepository;
        this.historyRepository = historyRepository;
        this.storage = storage;
        this.ruleService = ruleService;
        this.credentialService = credentialService;
        this.notificationService = notificationService;
        this.portalUrl = portalUrl;
        this.adminUserRepository = adminUserRepository;
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public VisaApplicationEntity get(Long id) {
        VisaApplicationEntity app = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid application Id: " + id));
        if (app.getDocuments() != null) app.getDocuments().size(); // initialise for the view
        return app;
    }

    @Transactional(readOnly = true)
    public List<VisaApplicationEntity> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ApplicationStatusHistoryEntity> history(Long applicationId) {
        VisaApplicationEntity app = get(applicationId);
        return historyRepository.findByApplicationTypeAndApplicationIdOrderByChangedAtAsc(app.getApplicationType(), app.getId());
    }

    @Transactional(readOnly = true)
    public ApplicationDocumentEntity document(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Document Id: " + documentId));
    }

    /** Required slots for the application's visa type, plus any extra documents uploaded outside a slot. */
    @Transactional(readOnly = true)
    public List<DocumentSlot> documentSlots(VisaApplicationEntity app) {
        List<ApplicationDocumentEntity> current = app.getCurrentDocuments();
        List<DocumentSlot> slots = new ArrayList<>();
        Set<String> covered = new HashSet<>();
        for (RequiredDocumentEntity rule : ruleService.requiredDocuments(app.getVisaTypeId(), app.getNationality())) {
            ApplicationDocumentEntity doc = latestFor(current, rule.getDocCode());
            slots.add(new DocumentSlot(rule, doc));
            covered.add(rule.getDocCode());
        }
        for (ApplicationDocumentEntity doc : current) {
            if (doc.getDocCode() == null || !covered.contains(doc.getDocCode())) {
                slots.add(new DocumentSlot(null, doc));
            }
        }
        return slots;
    }

    /** True when every mandatory slot holds a verified file (legacy apps: all current files verified). */
    @Transactional(readOnly = true)
    public boolean allDocumentsVerified(VisaApplicationEntity app) {
        List<DocumentSlot> slots = documentSlots(app);
        if (slots.isEmpty()) return false;
        return slots.stream().filter(DocumentSlot::isMandatory).allMatch(DocumentSlot::isSatisfied);
    }

    @Transactional(readOnly = true)
    public List<String> documentBlockers(VisaApplicationEntity app) {
        List<String> blockers = new ArrayList<>();
        List<DocumentSlot> slots = documentSlots(app);
        if (slots.isEmpty()) blockers.add("No documents uploaded");
        for (DocumentSlot slot : slots) {
            if (!slot.isMandatory() || slot.isSatisfied()) continue;
            if (slot.current() == null) blockers.add(slot.getLabel() + ": missing");
            else blockers.add(slot.getLabel() + ": " + slot.current().getStatusEnum().name().toLowerCase());
        }
        return blockers;
    }

    private static ApplicationDocumentEntity latestFor(List<ApplicationDocumentEntity> docs, String code) {
        return docs.stream()
                .filter(d -> code.equals(d.getDocCode()))
                .max(Comparator.comparing(ApplicationDocumentEntity::getVersion, Comparator.nullsFirst(Integer::compare))
                        .thenComparing(ApplicationDocumentEntity::getId))
                .orElse(null);
    }

    // ------------------------------------------------------------------ submission

    /**
     * Saves a new application with its required documents, records the first status and
     * issues the applicant's temporary portal credentials. Any failure rolls the whole thing back.
     */
    @Transactional
    public SubmissionResult submit(VisaApplicationEntity app, Map<String, MultipartFile> files) {
        List<String> errors = new ArrayList<>();
        VisaTypeEntity type = ruleService.visaType(app.getVisaTypeId()).orElse(null);
        if (type == null || !Boolean.TRUE.equals(type.getActive())
                || !type.getCountry().getCountryCode().equalsIgnoreCase(app.getCountryCode())) {
            errors.add("Please choose a valid visa type.");
        } else if (app instanceof IndiaVisaApplicationEntity india) {
            india.setVisaService(type.getName());
        }
        // customer_accounts.customer_email is required: the passkey and status emails go there
        if (app.getEmailId() == null || app.getEmailId().isBlank()) {
            errors.add("Email address is required.");
        }
        if (app.getPassportNumber() == null || app.getPassportNumber().isBlank()) {
            errors.add("Passport number is required.");
        } else {
            app.setPassportNumber(app.getPassportNumber().trim().toUpperCase());
            // passport_number is unique within each destination's table
            boolean duplicate = repository.findByPassportNumber(app.getPassportNumber()).stream()
                    .anyMatch(other -> other.getCountryCode().equals(app.getCountryCode()));
            if (duplicate) {
                errors.add("An application for passport " + app.getPassportNumber() + " already exists. "
                        + "Please sign in to the applicant portal to check it.");
            }
        }

        List<RequiredDocumentEntity> rules = ruleService.requiredDocuments(app.getVisaTypeId(), app.getNationality());
        Map<RequiredDocumentEntity, MultipartFile> toStore = new LinkedHashMap<>();
        for (RequiredDocumentEntity rule : rules) {
            MultipartFile file = files.get("doc_" + rule.getDocCode());
            if (file == null || file.isEmpty()) {
                if (Boolean.TRUE.equals(rule.getMandatory())) errors.add(rule.getLabel() + " is required.");
                continue;
            }
            try {
                storage.validate(file, rule, rule.getLabel());
                toStore.put(rule, file);
            } catch (ValidationException e) {
                errors.addAll(e.getErrors());
            }
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);

        app.setId(null);
        app.setStatus(ApplicationStatus.PENDING.name());
        repository.save(app);

        toStore.forEach((rule, file) -> attach(app, file, rule.getDocCode(), "applicant"));
        historyRepository.save(new ApplicationStatusHistoryEntity(app, null, ApplicationStatus.PENDING.name(),
                "applicant", "Application submitted online"));

        IssuedCredentials credentials = credentialService.issue(app.getId(), app.getCountryCode(), app.getEmailId());
        return new SubmissionResult(app, credentials);
    }

    private ApplicationDocumentEntity attach(VisaApplicationEntity app, MultipartFile file, String docCode, String uploadedBy) {
        DocumentStorageService.StoredFile stored = storage.store(file, uploadDir(app));
        // A new file for the same slot supersedes the previous one but keeps it for history
        ApplicationDocumentEntity previous = OTHER_DOC_CODE.equals(docCode) ? null : latestFor(app.getCurrentDocuments(), docCode);
        ApplicationDocumentEntity doc = app.newDocument();
        doc.setFileName(stored.originalName());
        doc.setFileType(stored.contentType());
        doc.setFilePath(stored.path());
        doc.setFileSize(stored.size());
        doc.setDocCode(docCode);
        doc.setUploadedBy(uploadedBy);
        doc.setStatus(DocumentStatus.UPLOADED.name());

        if (previous != null) {
            previous.setStatus(DocumentStatus.SUPERSEDED.name());
            doc.setVersion((previous.getVersion() == null ? 1 : previous.getVersion()) + 1);
            doc.setPreviousVersionId(previous.getId());
        } else {
            doc.setVersion(1);
        }
        return documentRepository.save(doc);
    }

    // ------------------------------------------------------------------ operator edits & documents

    @Transactional
    public void updateDetails(VisaApplicationEntity form, String actor) {
        VisaApplicationEntity existing = get(form.getId());
        existing.setGivenNames(form.getGivenNames());
        existing.setSurname(form.getSurname());
        // Operators see a masked number; only overwrite when a real value was typed in
        if (form.getPassportNumber() != null && !form.getPassportNumber().isBlank() && !MaskingUtil.isMasked(form.getPassportNumber())) {
            existing.setPassportNumber(form.getPassportNumber().trim().toUpperCase());
        }
        existing.setEmailId(form.getEmailId());
        if (form.getPassportDateOfExpiry() != null) existing.setPassportDateOfExpiry(form.getPassportDateOfExpiry());
        if (existing instanceof IndiaVisaApplicationEntity india && form instanceof IndiaVisaApplicationEntity f) {
            if (f.getExpectedDateOfArrival() != null) india.setExpectedDateOfArrival(f.getExpectedDateOfArrival());
            india.setPresentHouseNo(f.getPresentHouseNo());
            india.setPresentVillageTownCity(f.getPresentVillageTownCity());
            india.setPresentCountry(f.getPresentCountry());
            india.setPresentMobileNo(f.getPresentMobileNo());
        } else if (existing instanceof SriLankaVisaApplicationEntity sl && form instanceof SriLankaVisaApplicationEntity f) {
            if (f.getIntendedArrivalDate() != null) sl.setIntendedArrivalDate(f.getIntendedArrivalDate());
            sl.setContactNumber(f.getContactNumber());
        }
    }

    /** Operator or customer upload into a slot (docCode) or as an extra document (OTHER). */
    @Transactional
    public ApplicationDocumentEntity uploadDocument(Long applicationId, String docCode, MultipartFile file, String uploadedBy) {
        VisaApplicationEntity app = get(applicationId);
        String code = docCode == null || docCode.isBlank() ? OTHER_DOC_CODE : docCode;
        RequiredDocumentEntity rule = ruleService.requiredDocuments(app.getVisaTypeId(), app.getNationality()).stream()
                .filter(r -> r.getDocCode().equals(code)).findFirst().orElse(null);
        storage.validate(file, rule, rule != null ? rule.getLabel() : "Document");
        return attach(app, file, code, uploadedBy);
    }

    /** Customer upload: only into a slot that is missing, rejected or invalidated. */
    @Transactional
    public void customerUpload(Long applicationId, String docCode, MultipartFile file, String referenceNo) {
        VisaApplicationEntity app = get(applicationId);
        if (app.getStatusEnum().isClosed() && !Boolean.TRUE.equals(app.getPassportInvalidated())) {
            throw new ValidationException("This application is closed; documents can no longer be changed.");
        }
        DocumentSlot slot = documentSlots(app).stream().filter(s -> docCode.equals(s.getDocCode())).findFirst()
                .orElseThrow(() -> new ValidationException("Unknown document type."));
        if (!slot.needsUpload()) {
            throw new ValidationException(slot.getLabel() + " has already been received and does not need replacing.");
        }
        uploadDocument(applicationId, docCode, file, referenceNo);
    }

    @Transactional
    public Long deleteDocument(Long documentId) {
        ApplicationDocumentEntity doc = document(documentId);
        VisaApplicationEntity app = doc.getVisaApplication();
        app.getDocuments().remove(doc);
        documentRepository.delete(doc);
        // Physical file goes only after the database delete has committed
        storage.deleteAfterCommit(doc.getFilePath());
        return app.getId();
    }

    @Transactional
    public Long verifyDocument(Long documentId, String actor) {
        ApplicationDocumentEntity doc = document(documentId);
        doc.setStatus(DocumentStatus.VERIFIED.name());
        doc.setRejectionReason(null);
        doc.setReviewedBy(actor);
        doc.setReviewedAt(LocalDateTime.now());
        VisaApplicationEntity app = doc.getVisaApplication();
        if (PASSPORT_DOC_CODE.equals(doc.getDocCode()) && Boolean.TRUE.equals(app.getPassportInvalidated())) {
            app.setPassportInvalidated(false); // replacement passport accepted after a loss
        }
        return app.getId();
    }

    @Transactional
    public Long rejectDocument(Long documentId, String reason, String actor) {
        if (reason == null || reason.isBlank()) throw new ValidationException("A rejection reason is required.");
        ApplicationDocumentEntity doc = document(documentId);
        doc.setStatus(DocumentStatus.REJECTED.name());
        doc.setRejectionReason(reason.trim());
        doc.setReviewedBy(actor);
        doc.setReviewedAt(LocalDateTime.now());
        VisaApplicationEntity app = doc.getVisaApplication();
        notifyCustomer(app, "Document re-upload requested",
                "Our team could not accept your document \"" + doc.getFileName() + "\".\n\nReason: " + reason.trim()
                        + "\n\nPlease sign in to the applicant portal and upload a replacement.");
        return app.getId();
    }

    // ------------------------------------------------------------------ status workflow

    /** Business rules for moving an application to a new status; empty when the move is allowed. */
    @Transactional(readOnly = true)
    public List<String> transitionErrors(VisaApplicationEntity app, ApplicationStatus target, String reason) {
        ApplicationStatus from = app.getStatusEnum();
        if (from == target) return List.of("The application is already " + target.getLabel() + ".");
        List<String> errors = new ArrayList<>();
        switch (target) {
            case SUBMITTED -> {
                if (!allDocumentsVerified(app)) {
                    errors.add("All mandatory documents must be VERIFIED before submitting to the government portal.");
                    errors.addAll(documentBlockers(app));
                }
                if (app.getGovReferenceNo() == null || app.getGovReferenceNo().isBlank()) {
                    errors.add("Record the government portal reference number first.");
                }
            }
            case APPROVED -> {
                if (from != ApplicationStatus.SUBMITTED) errors.add("Only applications submitted to the government can be approved.");
                if (app.getApprovedVisaPath() == null) errors.add("Upload the approved visa PDF first.");
                if (app.getGrantDate() == null) errors.add("Record the visa grant date first.");
            }
            case REJECTED -> {
                if (reason == null || reason.isBlank()) errors.add("A reason is required when rejecting an application.");
            }
            case PENDING -> {
                if (reason == null || reason.isBlank()) errors.add("A reason is required when reopening an application.");
            }
        }
        return errors;
    }

    @Transactional
    public void changeStatus(Long applicationId, ApplicationStatus target, String reason, String actor) {
        VisaApplicationEntity app = get(applicationId);
        ApplicationStatus from = app.getStatusEnum();
        List<String> errors = transitionErrors(app, target, reason);
        if (!errors.isEmpty()) throw new ValidationException(errors);

        app.setStatus(target.name());
        if (target == ApplicationStatus.SUBMITTED && app.getGovSubmissionDate() == null) {
            app.setGovSubmissionDate(LocalDate.now());
        }
        historyRepository.save(new ApplicationStatusHistoryEntity(app, from.name(), target.name(), actor,
                reason == null || reason.isBlank() ? null : reason.trim()));

        switch (target) {
            case REJECTED -> {
                notifyCustomer(app, "Visa application update",
                        "We are sorry, your visa application was not successful.\n\nReason: " + reason.trim());
                credentialService.deactivateForApplication(app.getId(), "Application rejected");
            }
            case APPROVED -> notifyCustomer(app, "Your visa has been approved",
                    "Good news: your visa has been approved. You can download it from the applicant portal. "
                            + "Please complete your pre-departure checklist (insurance and health declaration) there as well.");
            case SUBMITTED -> notifyCustomer(app, "Visa application submitted",
                    "Your application has been reviewed and submitted to the government portal (reference "
                            + app.getGovReferenceNo() + ").");
            default -> { }
        }
    }

    @Transactional
    public void recordGovReference(Long applicationId, String govReferenceNo) {
        if (govReferenceNo == null || govReferenceNo.isBlank()) throw new ValidationException("Reference number is required.");
        VisaApplicationEntity app = get(applicationId);
        app.setGovReferenceNo(govReferenceNo.trim());
    }

    /** Records the grant details and approved visa PDF, then moves the application to APPROVED. */
    @Transactional
    public void recordGrant(Long applicationId, LocalDate grantDate, LocalDate validFrom, LocalDate validTo,
                            String entryType, MultipartFile approvedVisa, String actor) {
        VisaApplicationEntity app = get(applicationId);
        List<String> errors = new ArrayList<>();
        if (grantDate == null) errors.add("Grant date is required.");
        if (validFrom == null || validTo == null) errors.add("Visa validity dates are required.");
        else if (validTo.isBefore(validFrom)) errors.add("Visa 'valid to' must be after 'valid from'.");
        try {
            EntryType.valueOf(entryType);
        } catch (Exception e) {
            errors.add("Choose an entry type.");
        }
        if (approvedVisa == null || approvedVisa.isEmpty()) {
            if (app.getApprovedVisaPath() == null) errors.add("Upload the approved visa PDF.");
        } else {
            RequiredDocumentEntity pdfOnly = new RequiredDocumentEntity();
            pdfOnly.setAllowedFormats("pdf");
            pdfOnly.setMaxSizeMb(10);
            try {
                storage.validate(approvedVisa, pdfOnly, "Approved visa");
            } catch (ValidationException e) {
                errors.addAll(e.getErrors());
            }
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);

        if (approvedVisa != null && !approvedVisa.isEmpty()) {
            String old = app.getApprovedVisaPath();
            app.setApprovedVisaPath(storage.store(approvedVisa, uploadDir(app) + "/approved").path());
            storage.deleteAfterCommit(old);
        }
        app.setGrantDate(grantDate);
        app.setVisaValidFrom(validFrom);
        app.setVisaValidTo(validTo);
        app.setEntryType(entryType);
        if (app.getStatusEnum() != ApplicationStatus.APPROVED) {
            changeStatus(applicationId, ApplicationStatus.APPROVED, "Visa granted", actor);
        }
    }

    @Transactional
    public void assign(Long applicationId, String operatorUsername) {
        VisaApplicationEntity app = get(applicationId);
        if (operatorUsername == null || operatorUsername.isBlank()) {
            app.setAssignedOperatorUser(null);
            return;
        }
        AdminUserEntity operator = adminUserRepository.findByUsername(operatorUsername);
        if (operator == null) throw new ValidationException("Unknown staff member " + operatorUsername + ".");
        app.setAssignedOperatorUser(operator);
    }

    // ------------------------------------------------------------------ helpers

    /** Uploads are kept per destination, e.g. uploads/in, uploads/lk. */
    private static String uploadDir(VisaApplicationEntity app) {
        return app.getCountryCode().toLowerCase();
    }

    public void notifyCustomer(VisaApplicationEntity app, String subject, String body) {
        String reference = credentialService.findByApplication(app.getId()).map(a -> a.getReferenceNo()).orElse("");
        notificationService.send(app.getEmailId(), subject + (reference.isEmpty() ? "" : " (" + reference + ")"),
                "Dear " + app.getFullName() + ",\n\n" + body + "\n\nApplicant portal: " + portalUrl);
    }
}
