package com.travel.module1_auth.controller;

import com.travel.module1_auth.service.AdminUserService;
import com.travel.common.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "module1_auth/admin-users";
    }

    @PostMapping("/add")
    public String addUser(@RequestParam String username,
                          @RequestParam(required = false) String fullName,
                          @RequestParam(required = false) String email,
                          @RequestParam String password,
                          @RequestParam String role,
                          Authentication auth, HttpServletRequest request, RedirectAttributes flash) {
        try {
            userService.createUser(username, fullName, email, password, role, auth.getName(), RequestUtils.clientIp(request));
            flash.addFlashAttribute("success", "User '" + username.trim() + "' created.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/update")
    public String updateUser(@PathVariable Long id,
                             @RequestParam(required = false) String fullName,
                             @RequestParam(required = false) String email,
                             @RequestParam String role,
                             Authentication auth, HttpServletRequest request, RedirectAttributes flash) {
        try {
            userService.updateProfile(id, fullName, email, role, auth.getName(), RequestUtils.clientIp(request));
            flash.addFlashAttribute("success", "User updated.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, Authentication auth, HttpServletRequest request, RedirectAttributes flash) {
        return setActive(id, false, auth, request, flash);
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, Authentication auth, HttpServletRequest request, RedirectAttributes flash) {
        return setActive(id, true, auth, request, flash);
    }

    private String setActive(Long id, boolean active, Authentication auth, HttpServletRequest request, RedirectAttributes flash) {
        try {
            userService.setActive(id, active, auth.getName(), RequestUtils.clientIp(request));
            flash.addFlashAttribute("success", active ? "Account reactivated." : "Account deactivated. Its audit history is kept.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, Authentication auth, HttpServletRequest request, RedirectAttributes flash) {
        String temporary = userService.resetPassword(id, auth.getName(), RequestUtils.clientIp(request));
        flash.addFlashAttribute("success", "Temporary password for '" + userService.get(id).getUsername() + "': "
                + temporary + "  (shown once; the user must change it at next login)");
        return "redirect:/admin/users";
    }
}
