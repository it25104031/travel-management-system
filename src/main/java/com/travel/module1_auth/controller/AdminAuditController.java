package com.travel.module1_auth.controller;

import com.travel.module1_auth.service.AuthAuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminAuditController {

    private final AuthAuditService auditService;

    public AdminAuditController(AuthAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/admin/audit")
    public String auditLog(@RequestParam(required = false) String username, Model model) {
        model.addAttribute("entries", auditService.recent(username));
        model.addAttribute("username", username);
        return "module1_auth/admin-audit";
    }
}
