package com.travel.module1_auth.controller;

import com.travel.module1_auth.service.TempCredentialService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomerPortalAuthController {

    private final TempCredentialService credentialService;

    public CustomerPortalAuthController(TempCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping("/portal/login")
    public String loginPage() {
        return "module1_auth/portal-login";
    }

    /** "Prompt renewal" from the activity diagram: emails a fresh passkey if the email matches the one on file. */
    @PostMapping("/portal/renew")
    public String renew(@RequestParam String referenceNo, @RequestParam String email, RedirectAttributes flash) {
        credentialService.renew(referenceNo, email);
        // Same message either way so the form cannot be used to discover valid reference numbers
        flash.addFlashAttribute("info", "If the reference number and email match our records, a new passkey has been emailed to you.");
        return "redirect:/portal/login";
    }
}
