package com.travel.module2_visa.controller;

import com.travel.common.FlashMessages;
import com.travel.module2_visa.domain.ApplicationStatus;
import com.travel.module2_visa.domain.IncidentSeverity;
import com.travel.module2_visa.domain.IncidentType;
import com.travel.module2_visa.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/** Group departures: batching travellers, readiness, the booking gate and manifests. */
@Controller
@RequestMapping("/visas/departures")
public class DepartureController {

    private final DepartureService departureService;
    private final ReadinessService readinessService;
    private final ManifestService manifestService;
    private final IncidentService incidentService;
    private final VisaRuleService ruleService;

    public DepartureController(DepartureService departureService, ReadinessService readinessService,
                               ManifestService manifestService, IncidentService incidentService, VisaRuleService ruleService) {
        this.ruleService = ruleService;
        this.departureService = departureService;
        this.readinessService = readinessService;
        this.manifestService = manifestService;
        this.incidentService = incidentService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("departures", departureService.all().stream()
                .map(d -> readinessService.refresh(d.getId())).toList());
        model.addAttribute("countries", ruleService.activeCountries());
        return "module2_visa/departures";
    }

    @PostMapping
    public String create(@RequestParam String code, @RequestParam String tourName,
                         @RequestParam(required = false) String destinationCountry,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
                         RedirectAttributes flash) {
        FlashMessages.run(flash, "Departure created.", () -> departureService.create(code, tourName, destinationCountry, departureDate, returnDate));
        return "redirect:/visas/departures";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("readiness", readinessService.refresh(id));
        model.addAttribute("unassigned", departureService.unassignedApplications());
        model.addAttribute("incidents", incidentService.forDeparture(id));
        model.addAttribute("incidentTypes", IncidentType.values());
        model.addAttribute("statuses", ApplicationStatus.values());
        model.addAttribute("severities", IncidentSeverity.values());
        return "module2_visa/departure-detail";
    }

    @PostMapping("/{id}/travellers")
    public String addTravellers(@PathVariable Long id, @RequestParam(required = false) List<Long> applicationIds, RedirectAttributes flash) {
        FlashMessages.run(flash, "Travellers added.", () -> departureService.addTravellers(id, applicationIds));
        return "redirect:/visas/departures/" + id;
    }

    @PostMapping("/{id}/travellers/{appId}/remove")
    public String removeTraveller(@PathVariable Long id, @PathVariable Long appId, RedirectAttributes flash) {
        FlashMessages.run(flash, "Traveller removed from the departure.", () -> departureService.removeTraveller(id, appId));
        return "redirect:/visas/departures/" + id;
    }

    @PostMapping("/{id}/copy-details")
    public String copyDetails(@PathVariable Long id, @RequestParam Long leadApplicationId, RedirectAttributes flash) {
        FlashMessages.run(flash, "Shared address and itinerary copied from the lead traveller.",
                () -> departureService.copySharedDetails(id, leadApplicationId));
        return "redirect:/visas/departures/" + id;
    }

    @PostMapping("/{id}/batch-status")
    public String batchStatus(@PathVariable Long id, @RequestParam(required = false) List<Long> applicationIds,
                              @RequestParam ApplicationStatus status, @RequestParam(required = false) String reason,
                              Authentication auth, RedirectAttributes flash) {
        int[] changed = new int[1];
        FlashMessages.run(flash, null, () -> changed[0] = departureService.batchChangeStatus(id, applicationIds, status, reason, auth.getName()));
        if (changed[0] > 0) flash.addFlashAttribute("success", changed[0] + " traveller(s) moved to " + status.getLabel() + ".");
        return "redirect:/visas/departures/" + id;
    }

    @PostMapping("/{id}/ready")
    public String markReady(@PathVariable Long id, Authentication auth, RedirectAttributes flash) {
        FlashMessages.run(flash, "Booking gate unlocked: departure cleared for travel.", () -> readinessService.markReady(id, auth.getName()));
        return "redirect:/visas/departures/" + id;
    }

    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable Long id, RedirectAttributes flash) {
        FlashMessages.run(flash, "Departure reopened.", () -> departureService.reopen(id));
        return "redirect:/visas/departures/" + id;
    }

    @PostMapping("/{id}/departed")
    public String departed(@PathVariable Long id, RedirectAttributes flash) {
        FlashMessages.run(flash, "Departure closed; travellers' temporary portal access has ended.", () -> departureService.markDeparted(id));
        return "redirect:/visas/departures/" + id;
    }

    @PostMapping("/{id}/reminders")
    public String reminders(@PathVariable Long id, RedirectAttributes flash) {
        int[] sent = new int[1];
        FlashMessages.run(flash, null, () -> sent[0] = readinessService.sendReminders(id));
        flash.addFlashAttribute("success", "Reminders sent to " + sent[0] + " traveller(s) with outstanding items.");
        return "redirect:/visas/departures/" + id;
    }

    @GetMapping("/{id}/manifest.csv")
    public ResponseEntity<byte[]> manifestCsv(@PathVariable Long id) {
        String code = departureService.get(id).getCode();
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("manifest-" + code + ".csv").build().toString())
                .body(manifestService.csv(id));
    }

    @GetMapping("/{id}/manifest.pdf")
    public ResponseEntity<byte[]> manifestPdf(@PathVariable Long id) {
        String code = departureService.get(id).getCode();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("manifest-" + code + ".pdf").build().toString())
                .body(manifestService.pdf(id));
    }
}
