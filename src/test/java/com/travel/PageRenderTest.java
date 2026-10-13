package com.travel;

import com.travel.module2_visa.domain.IncidentSeverity;
import com.travel.module2_visa.domain.IncidentType;
import com.travel.module2_visa.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Renders every page with real data so template errors are caught by the build. */
@SpringBootTest
@AutoConfigureMockMvc
class PageRenderTest {

    // Visa applications need a signed-in website traveller (or staff)
    static final org.springframework.test.web.servlet.request.RequestPostProcessor TRAVELLER =
            org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("traveller@example.com").roles("MEMBER");

    @Autowired MockMvc mvc;
    @Autowired VisaApplicationService applicationService;
    @Autowired VisaRuleService ruleService;
    @Autowired DepartureService departureService;
    @Autowired ComplianceService complianceService;
    @Autowired IncidentService incidentService;

    @Test
    void applicantCanApplyThroughTheWebForm() throws Exception {
        Long typeId = TestData.touristType(ruleService).getId();
        mvc.perform(get("/visas/apply/india").param("visaTypeId", typeId.toString()).param("nationality", "Sri Lankan").with(TRAVELLER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"doc_PHOTO\"")))
                .andExpect(content().string(containsString("name=\"doc_PASSPORT_BIO\"")));

        MvcResult result = mvc.perform(multipart("/visas/apply/india").with(TRAVELLER)
                        .file(TestData.photo("doc_PHOTO"))
                        .file(TestData.pdf("doc_PASSPORT_BIO", "passport.pdf"))
                        .param("visaTypeId", typeId.toString())
                        .param("nationality", "Sri Lankan")
                        .param("givenNames", "Web").param("surname", "Applicant")
                        .param("emailId", "web@example.com")
                        .param("passportNumber", "WEB12345")
                        .param("passportDateOfExpiry", LocalDate.now().plusYears(4).toString())
                        .param("expectedDateOfArrival", LocalDate.now().plusMonths(1).toString())
                        .param("status", "APPROVED") // must be ignored
                        .with(csrf()))
                .andExpect(redirectedUrl("/visas/apply/submitted")).andReturn();

        mvc.perform(get("/visas/apply/submitted").flashAttrs(result.getFlashMap()).with(TRAVELLER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reference number")));

        boolean pending = applicationService.findAll().stream()
                .filter(a -> "WEB12345".equals(a.getPassportNumber()))
                .allMatch(a -> a.getStatus().equals("PENDING"));
        assertTrue(pending, "status from the form post must be ignored");
    }

    @Test
    void invalidSubmissionReRendersTheFormWithErrors() throws Exception {
        Long typeId = TestData.touristType(ruleService).getId();
        mvc.perform(multipart("/visas/apply/india").with(TRAVELLER)
                        .param("visaTypeId", typeId.toString()).param("nationality", "Indian")
                        .param("passportNumber", "NOFILES1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("is required")));
    }

    @Test
    void staffAndPortalPagesRenderWithData() throws Exception {
        var submission = TestData.submit(applicationService, ruleService);
        Long id = submission.application().getId();
        var departure = departureService.create("RENDER-" + id, "Render Tour", "India", LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(1).plusDays(5));
        departureService.addTravellers(departure.getId(), List.of(id));
        complianceService.saveInsurance(id, "Provider", "P-1", LocalDate.now(), LocalDate.now().plusYears(1));
        complianceService.saveHealth(id, true, "Yellow fever, 2025", "None", "Contact", "123");
        applicationService.rejectDocument(applicationService.get(id).getCurrentDocuments().get(0).getId(), "Blurry", "operator");
        incidentService.log(departure.getId(), id, IncidentType.LOST_PASSPORT, IncidentSeverity.HIGH, "Lost", null, "admin");

        for (String role : new String[]{"ADMIN", "USER"}) {
            var u = user(role.toLowerCase()).roles(role);
            mvc.perform(get("/visas/dashboard").param("filter", "all").with(u)).andExpect(status().isOk());
            mvc.perform(get("/visas/dashboard/" + id).with(u)).andExpect(status().isOk());
            mvc.perform(get("/visas/dashboard/" + id).param("reveal", "true").with(u)).andExpect(status().isOk());
            mvc.perform(get("/visas/departures").with(u)).andExpect(status().isOk());
            mvc.perform(get("/visas/departures/" + departure.getId()).with(u)).andExpect(status().isOk());
            mvc.perform(get("/visas/incidents").with(u)).andExpect(status().isOk());
            mvc.perform(get("/visas/departures/" + departure.getId() + "/manifest.pdf").with(u)).andExpect(status().isOk());
            mvc.perform(get("/visas/departures/" + departure.getId() + "/manifest.csv").with(u)).andExpect(status().isOk());
            mvc.perform(get("/files/documents/" + applicationService.get(id).getDocuments().get(0).getId()).with(u)).andExpect(status().isOk());
        }
        // Operators see masked passport numbers in the queue
        mvc.perform(get("/visas/dashboard").param("filter", "all").with(user("operator").roles("USER")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(submission.application().getPassportNumber()))));
        mvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN"))).andExpect(redirectedUrl("/staff"));
        mvc.perform(get("/staff").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/reports").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());

        MvcResult login = mvc.perform(post("/portal/login").with(csrf())
                        .param("referenceNo", submission.credentials().referenceNo()).param("passkey", submission.credentials().passkey()))
                .andExpect(redirectedUrl("/portal/case")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/portal/case").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Upload replacement")));
        mvc.perform(get("/portal/compliance").session(session)).andExpect(status().isOk());
    }
}
