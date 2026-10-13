package com.travel;

import com.travel.module1_auth.domain.AuthEventType;
import com.travel.module1_auth.repository.AuthAuditLogRepository;
import com.travel.common.ValidationException;
import com.travel.module2_visa.domain.ApplicationStatus;
import com.travel.module2_visa.domain.SriLankaVisaApplicationEntity;
import com.travel.module2_visa.domain.VisaApplicationEntity;
import com.travel.module2_visa.service.DepartureService;
import com.travel.module2_visa.service.VisaApplicationService;
import com.travel.module2_visa.service.VisaRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MultiCountryAndBatchTest {

    // Visa applications need a signed-in website traveller (or staff)
    static final org.springframework.test.web.servlet.request.RequestPostProcessor TRAVELLER =
            org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("traveller@example.com").roles("MEMBER");

    @Autowired TransactionTemplate tx;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired VisaApplicationService applicationService;
    @Autowired VisaRuleService ruleService;
    @Autowired DepartureService departureService;
    @Autowired AuthAuditLogRepository auditRepository;
    @Autowired ApplicationEventPublisher events;

    @Test
    void sriLankaEtaCanBeSubmittedThroughItsOwnForm() throws Exception {
        Long typeId = TestData.sriLankaTouristType(ruleService).getId();
        mvc.perform(get("/visas/select-country").with(TRAVELLER)).andExpect(content().string(containsString("/visas/apply/sri-lanka")));
        mvc.perform(get("/visas/apply/sri-lanka").with(TRAVELLER)).andExpect(status().isOk()).andExpect(content().string(containsString("Tourist ETA")));
        mvc.perform(get("/visas/apply/sri-lanka").param("visaTypeId", typeId.toString()).param("nationality", "Indian").with(TRAVELLER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Electronic Travel Authorization")))
                .andExpect(content().string(containsString("name=\"doc_PASSPORT_BIO\"")))
                .andExpect(content().string(containsString("name=\"doc_RETURN_TICKET\"")));

        MvcResult result = mvc.perform(multipart("/visas/apply/sri-lanka").with(TRAVELLER)
                        .file(TestData.pdf("doc_PASSPORT_BIO", "passport.pdf"))
                        .param("visaTypeId", typeId.toString()).param("nationality", "Indian")
                        .param("givenNames", "Ravi").param("surname", "Kumar").param("emailId", "ravi@example.com")
                        .param("passportNumber", "LKTEST001").param("purposeOfVisit", "TOURISM")
                        .param("contactNumber", "+94 77 123 4567")
                        .param("passportDateOfExpiry", LocalDate.now().plusYears(5).toString())
                        .param("intendedArrivalDate", LocalDate.now().plusMonths(1).toString())
                        .param("countryCode", "IN") // must be ignored: destination comes from the URL
                        .with(csrf()))
                .andExpect(redirectedUrl("/visas/apply/submitted")).andReturn();

        var creds = (com.travel.module1_auth.service.TempCredentialService.IssuedCredentials) result.getFlashMap().get("credentials");
        assertTrue(creds.referenceNo().startsWith("LK-"), creds.referenceNo());
        VisaApplicationEntity saved = applicationService.findAll().stream()
                .filter(a -> "LKTEST001".equals(a.getPassportNumber())).findFirst().orElseThrow();
        assertEquals("LK", saved.getCountryCode());
        assertTrue(saved instanceof SriLankaVisaApplicationEntity);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM srilanka_visa_applications WHERE passport_number = 'LKTEST001' "
                + "AND contact_number = '+94 77 123 4567' AND intended_arrival_date IS NOT NULL", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM india_visa_applications WHERE passport_number = 'LKTEST001'", Integer.class));
        String firstDocPath = tx.execute(st -> applicationService.get(saved.getId()).getDocuments().get(0).getFilePath());
        assertTrue(firstDocPath.contains("/lk/"), firstDocPath);

        mvc.perform(get("/visas/dashboard/" + saved.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("+94 77 123 4567")));
    }

    @Test
    void passportIsUniquePerDestinationOnly() {
        var india = TestData.newApplication(TestData.touristType(ruleService).getId());
        applicationService.submit(india, TestData.requiredFiles());

        var lk = TestData.newSriLankaApplication(TestData.sriLankaTouristType(ruleService).getId());
        lk.setPassportNumber(india.getPassportNumber());
        applicationService.submit(lk, Map.of("doc_PASSPORT_BIO", TestData.pdf("doc_PASSPORT_BIO", "p.pdf")));

        var again = TestData.newApplication(TestData.touristType(ruleService).getId());
        again.setPassportNumber(india.getPassportNumber());
        assertThrows(ValidationException.class, () -> applicationService.submit(again, TestData.requiredFiles()));
    }

    @Test
    void visaTypeMustBelongToTheDestination() {
        var app = TestData.newApplication(TestData.sriLankaTouristType(ruleService).getId()); // countryCode IN
        ValidationException e = assertThrows(ValidationException.class, () -> applicationService.submit(app, TestData.requiredFiles()));
        assertTrue(e.getErrors().get(0).contains("valid visa type"));
    }

    @Test
    void batchStatusChangeIsAllOrNothing() {
        var a = TestData.submit(applicationService, ruleService).application();
        var b = TestData.submit(applicationService, ruleService).application();
        var departure = departureService.create("BATCH-" + a.getId(), "Batch Tour", "India", LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(1).plusDays(3));
        departureService.addTravellers(departure.getId(), List.of(a.getId(), b.getId()));

        // Only a is ready for government submission
        applicationService.get(a.getId()).getCurrentDocuments().forEach(d -> applicationService.verifyDocument(d.getId(), "operator"));
        applicationService.recordGovReference(a.getId(), "G-A");
        applicationService.recordGovReference(b.getId(), "G-B");

        ValidationException e = assertThrows(ValidationException.class, () -> departureService.batchChangeStatus(
                departure.getId(), List.of(a.getId(), b.getId()), ApplicationStatus.SUBMITTED, null, "operator"));
        assertTrue(e.getErrors().stream().anyMatch(m -> m.startsWith(b.getFullName())));
        assertEquals(ApplicationStatus.PENDING, applicationService.get(a.getId()).getStatusEnum(), "nobody changes when one fails");

        int changed = departureService.batchChangeStatus(departure.getId(), List.of(a.getId(), b.getId()),
                ApplicationStatus.REJECTED, "Tour cancelled", "operator");
        assertEquals(2, changed);
        assertEquals(ApplicationStatus.REJECTED, applicationService.get(a.getId()).getStatusEnum());
        assertEquals(ApplicationStatus.REJECTED, applicationService.get(b.getId()).getStatusEnum());
    }

    @Test
    void idleTimeoutIsAuditedButExplicitLogoutIsNot() throws Exception {
        MockHttpSession idle = new MockHttpSession();
        idle.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(
                UsernamePasswordAuthenticationToken.authenticated("idle-user", null, List.of(new SimpleGrantedAuthority("ROLE_USER")))));
        events.publishEvent(new HttpSessionDestroyedEvent(idle));
        assertEquals(1, auditRepository.countByUsernameAndEventTypeAndCreatedAtAfter("idle-user", AuthEventType.SESSION_EXPIRED,
                java.time.LocalDateTime.now().minusMinutes(1)));

        MvcResult login = mvc.perform(post("/login").with(csrf()).param("username", "operator").param("password", "operator123"))
                .andExpect(redirectedUrl("/staff")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(redirectedUrl("/login?logout&staff"));
        events.publishEvent(new HttpSessionDestroyedEvent(session)); // what the container fires on invalidate
        assertEquals(0, auditRepository.countByUsernameAndEventTypeAndCreatedAtAfter("operator", AuthEventType.SESSION_EXPIRED,
                java.time.LocalDateTime.now().minusMinutes(1)));
    }
}
