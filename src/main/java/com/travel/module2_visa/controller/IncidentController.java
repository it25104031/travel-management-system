package com.travel.module2_visa.controller;

import com.travel.module1_auth.service.AdminUserService;
import com.travel.common.FlashMessages;
import com.travel.module2_visa.domain.IncidentSeverity;
import com.travel.module2_visa.domain.IncidentType;
import com.travel.module2_visa.service.DepartureService;
import com.travel.module2_visa.service.IncidentService;
import com.travel.module2_visa.service.VisaApplicationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/visas/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final DepartureService departureService;
    private final VisaApplicationService applicationService;
    private final AdminUserService userService;

    public IncidentController(IncidentService incidentService, DepartureService departureService,
                              VisaApplicationService applicationService, AdminUserService userService) {
        this.incidentService = incidentService;
        this.departureService = departureService;
        this.applicationService = applicationService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("incidents", incidentService.all());
        model.addAttribute("departures", departureService.all());
        model.addAttribute("applications", applicationService.findAll());
        model.addAttribute("incidentTypes", IncidentType.values());
        model.addAttribute("severities", IncidentSeverity.values());
        model.addAttribute("staff", userService.findAll().stream().filter(u -> u.isActiveAccount()).toList());
        return "module2_visa/incidents";
    }

    @PostMapping
    public String log(@RequestParam(required = false) Long departureId,
                      @RequestParam(required = false) Long applicationId,
                      @RequestParam IncidentType type,
                      @RequestParam IncidentSeverity severity,
                      @RequestParam String description,
                      @RequestParam(required = false) String assignedTo,
                      @RequestParam(required = false) String returnTo,
                      Authentication auth, RedirectAttributes flash) {
        String message = type == IncidentType.LOST_PASSPORT
                ? "Incident logged. The passport record was invalidated and a replacement checklist was sent to the traveller."
                : "Incident logged.";
        FlashMessages.run(flash, message,
                () -> incidentService.log(departureId, applicationId, type, severity, description, assignedTo, auth.getName()));
        return "departure".equals(returnTo) && departureId != null ? "redirect:/visas/departures/" + departureId : "redirect:/visas/incidents";
    }

    @PostMapping("/{id}/resolve")
    public String resolve(@PathVariable Long id, @RequestParam String resolution, RedirectAttributes flash) {
        FlashMessages.run(flash, "Incident resolved.", () -> incidentService.resolve(id, resolution));
        return "redirect:/visas/incidents";
    }
}
