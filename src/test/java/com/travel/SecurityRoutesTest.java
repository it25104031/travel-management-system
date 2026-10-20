package com.travel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityRoutesTest {

    @Autowired
    MockMvc mvc;

    @Test
    void publicPagesAreOpen() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/about")).andExpect(status().isOk());
        mvc.perform(get("/tours")).andExpect(status().isOk());
        mvc.perform(get("/cart")).andExpect(status().isOk());
        mvc.perform(get("/signup")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/portal/login")).andExpect(status().isOk());
    }

    @Test
    void visaApplicationsCheckoutAndProfileNeedASignIn() throws Exception {
        mvc.perform(get("/visas/select-country")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/visas/apply/india")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/checkout")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/my")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    void anonymousIsSentToTheRightLoginPage() throws Exception {
        mvc.perform(get("/visas/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/admin/users")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/portal/case")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/portal/login"));
    }

    @Test
    void uploadedFilesAreNotPublic() throws Exception {
        mvc.perform(get("/uploads/india/anything.jpg")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/files/documents/1")).andExpect(status().is3xxRedirection());
    }

    @Test
    void operatorCannotOpenAdminTools() throws Exception {
        mvc.perform(get("/admin/users").with(user("operator").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/visa-rules").with(user("operator").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/visas/dashboard").with(user("operator").roles("USER"))).andExpect(status().isOk());
        mvc.perform(get("/visas/departures").with(user("operator").roles("USER"))).andExpect(status().isOk());
    }

    @Test
    void adminCanOpenEverythingStaff() throws Exception {
        mvc.perform(get("/admin/users").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/audit").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/visa-rules").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/reports").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/countries").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/visas/incidents").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/account/password").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void customerCannotReachStaffPages() throws Exception {
        mvc.perform(get("/visas/dashboard").with(user("IN-2026-000001").roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/dashboard").with(user("IN-2026-000001").roles("CUSTOMER"))).andExpect(status().isForbidden());
    }

    @Test
    void staffCannotUseCustomerPortal() throws Exception {
        mvc.perform(get("/portal/case").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
    }
}
