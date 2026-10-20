package com.travel.module1_auth.controller;

import com.travel.module1_auth.service.SiteCustomerService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(name = "continue", required = false) String next,
                            @RequestParam(required = false) String staff,
                            Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return SiteCustomerService.isMember(auth) ? "redirect:/my" : "redirect:/staff";
        }
        model.addAttribute("staffMode", staff != null);
        model.addAttribute("continueTo", next);
        return "module1_auth/login";
    }
}
