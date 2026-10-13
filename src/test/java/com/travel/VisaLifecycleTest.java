package com.travel;

import com.travel.common.ValidationException;
import com.travel.module2_visa.domain.*;
import com.travel.module2_visa.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end compliance flow: submission, review, re-upload, approval, checklist, readiness gate, manifests. */
@SpringBootTest
class VisaLifecycleTest {

    @Autowired VisaApplicationService applicationService;
    @Autowired VisaRuleService ruleService;
    @Autowired ComplianceService complianceService;
    @Autowired DepartureService departureService;
    @Autowired ReadinessService readinessService;
    @Autowired IncidentService incidentService;
    @Autowired ManifestService manifestService;
    @Autowired DocumentStorageService storage;

    @Test
    void missingMandatoryDocumentBlocksSubmission() {
        var app = TestData.newApplication(TestData.touristType(ruleService).getId());
        ValidationException e = assertThrows(ValidationException.class,
                () -> applicationService.submit(app, Map.of("doc_PHOTO", TestData.photo("doc_PHOTO"))));
        assertTrue(e.getErrors().stream().anyMatch(m -> m.contains("Passport bio-data page")));
    }

    @Test
    void uploadRulesAreEnforced() {
        var app = TestData.newApplication(TestData.touristType(ruleService).getId());
        Map<String, MultipartFile> files = TestData.requiredFiles();
        files.put("doc_PHOTO", new MockMultipartFile("doc_PHOTO", "photo.png", "image/png", TestData.png(100, 100)));
        ValidationException e = assertThrows(ValidationException.class, () -> applicationService.submit(app, files));
        assertTrue(e.getErrors().get(0).contains("minimum is 350x350"));

        files.put("doc_PHOTO", new MockMultipartFile("doc_PHOTO", "virus.exe", "application/octet-stream", new byte[10]));
        e = assertThrows(ValidationException.class, () -> applicationService.submit(app, files));
        assertTrue(e.getErrors().get(0).contains("not allowed"));
    }

    @Test
    void cannotSubmitToGovernmentUntilDocumentsVerified() {
        var app = TestData.submit(applicationService, ruleService).application();
        applicationService.recordGovReference(app.getId(), "GOV-1");
        assertThrows(ValidationException.class,
                () -> applicationService.changeStatus(app.getId(), ApplicationStatus.SUBMITTED, null, "operator"));
    }

    @Test
    void rejectionRequiresReasonAndKeepsVersionHistory() {
        var app = TestData.submit(applicationService, ruleService).application();
        var photo = docByCode(app.getId(), "PHOTO");
        assertThrows(ValidationException.class, () -> applicationService.rejectDocument(photo.getId(), " ", "operator"));
        applicationService.rejectDocument(photo.getId(), "Background not white", "operator");

        applicationService.customerUpload(app.getId(), "PHOTO", TestData.photo("file"), "IN-REF");
        var reloaded = applicationService.get(app.getId());
        assertEquals(3, reloaded.getDocuments().size(), "old version kept for history");
        var current = docByCode(app.getId(), "PHOTO");
        assertEquals(2, current.getVersion());
        assertEquals(photo.getId(), current.getPreviousVersionId());
        assertEquals(DocumentStatus.SUPERSEDED, applicationService.document(photo.getId()).getStatusEnum());

        // A verified slot cannot be replaced by the customer
        applicationService.verifyDocument(current.getId(), "operator");
        assertThrows(ValidationException.class,
                () -> applicationService.customerUpload(app.getId(), "PHOTO", TestData.photo("file"), "IN-REF"));
    }

    @Test
    void deletingADocumentRemovesTheFileFromDisk() {
        var app = TestData.submit(applicationService, ruleService).application();
        var doc = docByCode(app.getId(), "PHOTO");
        Path file = storage.resolve(doc.getFilePath());
        assertTrue(Files.exists(file));
        applicationService.deleteDocument(doc.getId());
        assertFalse(Files.exists(file));
    }

    @Test
    void fullComplianceFlowUnlocksTheBookingGate() {
        var app = TestData.submit(applicationService, ruleService).application();
        Long id = app.getId();

        var departure = departureService.create("TEST-" + id, "Golden Triangle", "India", LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(2).plusDays(10));
        departureService.addTravellers(departure.getId(), List.of(id));

        // Incomplete group: gate stays locked
        assertThrows(ValidationException.class, () -> readinessService.markReady(departure.getId(), "operator"));

        applicationService.get(id).getCurrentDocuments().forEach(d -> applicationService.verifyDocument(d.getId(), "operator"));
        applicationService.recordGovReference(id, "IND-GOV-123");
        applicationService.changeStatus(id, ApplicationStatus.SUBMITTED, null, "operator");
        applicationService.recordGrant(id, LocalDate.now(), LocalDate.now(), LocalDate.now().plusYears(1), "DOUBLE",
                TestData.pdf("approvedVisa", "visa.pdf"), "operator");
        assertEquals(ApplicationStatus.APPROVED, applicationService.get(id).getStatusEnum());

        complianceService.saveInsurance(id, "Ceylinco", "POL-1", departure.getDepartureDate(), departure.getReturnDate().minusDays(1));
        // Cover ends a day early: operator cannot verify it
        assertThrows(ValidationException.class, () -> complianceService.reviewInsurance(id, true, null, "operator"));
        complianceService.saveInsurance(id, "Ceylinco", "POL-1", departure.getDepartureDate(), departure.getReturnDate());
        complianceService.reviewInsurance(id, true, null, "operator");
        complianceService.saveHealth(id, true, null, null, "Parent", "+94770000000");
        complianceService.reviewHealth(id, true, null, "operator");

        var readiness = readinessService.forDeparture(departure.getId());
        assertTrue(readiness.isComplete(), () -> readiness.travellers().get(0).getBlockers().toString());
        readinessService.markReady(departure.getId(), "operator");
        assertEquals(DepartureStatus.READY, departureService.get(departure.getId()).getStatusEnum());

        assertTrue(manifestService.pdf(departure.getId()).length > 500);
        String csv = new String(manifestService.csv(departure.getId()));
        assertTrue(csv.contains("IND-GOV-123"));

        // Lost passport: record invalidated and the gate re-locks
        incidentService.log(departure.getId(), id, IncidentType.LOST_PASSPORT, IncidentSeverity.HIGH, "Lost at hotel", null, "operator");
        assertEquals(DepartureStatus.PENDING, departureService.get(departure.getId()).getStatusEnum());
        assertEquals(DocumentStatus.INVALIDATED, docByCode(id, "PASSPORT_BIO").getStatusEnum());
        assertFalse(readinessService.forDeparture(departure.getId()).isComplete());

        // Replacement passport uploaded through the portal and verified clears the flag
        applicationService.customerUpload(id, "PASSPORT_BIO", TestData.pdf("file", "new-passport.pdf"), "IN-REF");
        applicationService.verifyDocument(docByCode(id, "PASSPORT_BIO").getId(), "operator");
        assertFalse(applicationService.get(id).getPassportInvalidated());
        assertTrue(readinessService.forDeparture(departure.getId()).isComplete());
    }

    private ApplicationDocumentEntity docByCode(Long appId, String code) {
        return applicationService.get(appId).getCurrentDocuments().stream()
                .filter(d -> code.equals(d.getDocCode())).findFirst().orElseThrow();
    }
}
