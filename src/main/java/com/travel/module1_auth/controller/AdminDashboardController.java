package com.travel.module1_auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** The old admin hub now lives in the shared staff workspace (/staff). */
@Controller
public class AdminDashboardController {

    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "redirect:/staff";
    }
}
