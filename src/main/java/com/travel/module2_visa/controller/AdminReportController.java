package com.travel.module2_visa.controller;

import com.travel.module2_visa.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/admin/reports")
    public String reports(Model model) {
        model.addAttribute("summary", reportService.summary());
        return "module2_visa/admin-reports";
    }
}
