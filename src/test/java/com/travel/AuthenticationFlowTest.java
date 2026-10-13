package com.travel;

import com.travel.module1_auth.domain.CustomerAccountEntity;
import com.travel.module1_auth.repository.CustomerAccountRepository;
import com.travel.module1_auth.service.AdminUserService;
import com.travel.module1_auth.service.TempCredentialService.IssuedCredentials;
import com.travel.module2_visa.service.VisaApplicationService;
import com.travel.module2_visa.service.VisaRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationFlowTest {

    @Autowired MockMvc mvc;
    @Autowired AdminUserService userService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired VisaApplicationService applicationService;
    @Autowired VisaRuleService ruleService;
    @Autowired CustomerAccountRepository customerAccounts;

    @Test
    void passwordsAreHashedWithBcryptStrengthAtLeastTen() {
        String hash = userService.findByUsername("operator").getPassword();
        assertTrue(hash.startsWith("$2a$12$") || hash.startsWith("$2b$12$"), hash);
        assertTrue(passwordEncoder.matches("operator123", hash));
    }

    @Test
    void adminFromTheTeamSchemaIsKeptAsIs() {
        // team-schema.sql inserts admin with its own BCrypt hash and role ADMIN; startup must not overwrite either
        var admin = userService.findByUsername("admin");
        assertTrue(admin.getPassword().startsWith("$2a$10$XGZ9grESLT0UzMW1Wh8JNOB"), admin.getPassword());
        assertEquals("ADMIN", admin.getRole());
        assertEquals("USER", userService.findByUsername("operator").getRole());
    }

    @Test
    void staffLoginRoutesByRole() throws Exception {
        if (userService.findByUsername("boss") == null) {
            userService.createUser("boss", "Boss", null, "boss-password-1", "ADMIN", "admin", null);
        }
        mvc.perform(post("/login").with(csrf()).param("username", "boss").param("password", "boss-password-1"))
                .andExpect(redirectedUrl("/staff"));
        mvc.perform(post("/login").with(csrf()).param("username", "operator").param("password", "operator123"))
                .andExpect(redirectedUrl("/staff"));
    }

    @Test
    void repeatedFailuresLockTheAccount() throws Exception {
        userService.createUser("locktest", "Lock Test", null, "correct-horse", "ROLE_USER", "admin", null);
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/login").with(csrf()).param("username", "locktest").param("password", "wrong"))
                    .andExpect(redirectedUrl("/login?error"));
        }
        // Even the right password is refused while locked
        mvc.perform(post("/login").with(csrf()).param("username", "locktest").param("password", "correct-horse"))
                .andExpect(redirectedUrl("/login?locked"));
    }

    @Test
    void deactivatedStaffCannotSignIn() throws Exception {
        var user = userService.createUser("leaver", "Leaver", null, "password-123", "ROLE_USER", "admin", null);
        userService.setActive(user.getId(), false, "admin", null);
        mvc.perform(post("/login").with(csrf()).param("username", "leaver").param("password", "password-123"))
                .andExpect(redirectedUrl("/login?disabled"));
    }

    @Test
    void forcedResetSendsUserToChangePassword() throws Exception {
        var user = userService.createUser("resetme", "Reset Me", null, "password-123", "ROLE_USER", "admin", null);
        String temporary = userService.resetPassword(user.getId(), "admin", null);
        mvc.perform(post("/login").with(csrf()).param("username", "resetme").param("password", temporary))
                .andExpect(redirectedUrl("/account/password?forced"));
        userService.changeOwnPassword("resetme", temporary, "brand-new-pass", "brand-new-pass", null);
        assertFalse(userService.findByUsername("resetme").getMustChangePassword());
        assertThrows(IllegalArgumentException.class,
                () -> userService.changeOwnPassword("resetme", "wrong-current", "another-pass", "another-pass", null));
    }

    @Test
    void customerSignsInWithReferenceAndPasskey() throws Exception {
        IssuedCredentials creds = TestData.submit(applicationService, ruleService).credentials();
        assertTrue(creds.referenceNo().startsWith("IN-"));
        CustomerAccountEntity account = customerAccounts.findByReferenceNo(creds.referenceNo()).orElseThrow();
        assertNotEquals(creds.passkey(), account.getPasskeyHash(), "passkey must be stored hashed");

        mvc.perform(post("/portal/login").with(csrf()).param("referenceNo", creds.referenceNo()).param("passkey", "wrong"))
                .andExpect(redirectedUrl("/portal/login?error"));

        MvcResult login = mvc.perform(post("/portal/login").with(csrf())
                        .param("referenceNo", creds.referenceNo().toLowerCase()).param("passkey", creds.passkey()))
                .andExpect(redirectedUrl("/portal/case")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/portal/case").session(session)).andExpect(status().isOk());
        mvc.perform(get("/visas/dashboard").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void expiredPasskeyIsRejectedAndEndsActiveSessions() throws Exception {
        IssuedCredentials creds = TestData.submit(applicationService, ruleService).credentials();
        MvcResult login = mvc.perform(post("/portal/login").with(csrf())
                        .param("referenceNo", creds.referenceNo()).param("passkey", creds.passkey()))
                .andExpect(redirectedUrl("/portal/case")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        CustomerAccountEntity account = customerAccounts.findByReferenceNo(creds.referenceNo()).orElseThrow();
        account.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        customerAccounts.save(account);

        mvc.perform(get("/portal/case").session(session)).andExpect(redirectedUrl("/portal/login?expired"));
        mvc.perform(post("/portal/login").with(csrf()).param("referenceNo", creds.referenceNo()).param("passkey", creds.passkey()))
                .andExpect(redirectedUrl("/portal/login?expired"));
    }

    @Test
    void customerCannotOpenAnotherApplicantsDocument() throws Exception {
        IssuedCredentials mine = TestData.submit(applicationService, ruleService).credentials();
        var other = TestData.submit(applicationService, ruleService).application();
        Long otherDocId = applicationService.get(other.getId()).getDocuments().get(0).getId();

        MvcResult login = mvc.perform(post("/portal/login").with(csrf())
                        .param("referenceNo", mine.referenceNo()).param("passkey", mine.passkey()))
                .andExpect(redirectedUrl("/portal/case")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get("/portal/documents/" + otherDocId + "/file").session(session)).andExpect(status().isForbidden());
    }
}
