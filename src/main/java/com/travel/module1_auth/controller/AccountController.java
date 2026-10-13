package com.travel.module1_auth.controller;

import com.travel.module1_auth.service.AdminUserService;
import com.travel.common.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Staff self-service: change own password after validating the current one. */
@Controller
@RequestMapping("/account")
public class AccountController {

    private final AdminUserService userService;

    public AccountController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/password")
    public String passwordForm(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());
        model.addAttribute("minLength", AdminUserService.MIN_PASSWORD_LENGTH);
        model.addAttribute("isAdmin", auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        return "module1_auth/account-password";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth, HttpServletRequest request, RedirectAttributes flash) {
        try {
            userService.changeOwnPassword(auth.getName(), currentPassword, newPassword, confirmPassword, RequestUtils.clientIp(request));
            flash.addFlashAttribute("success", "Your password has been updated.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account/password";
    }
}
