package com.travel.module2_visa.controller;

import com.travel.common.FlashMessages;
import com.travel.module2_visa.service.VisaRuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/visa-rules")
public class AdminVisaRuleController {

    private final VisaRuleService ruleService;

    public AdminVisaRuleController(VisaRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public String list(Model model) {
        var types = ruleService.allVisaTypes();
        model.addAttribute("countries", ruleService.allCountries());
        model.addAttribute("visaTypes", types);
        model.addAttribute("rulesByType", types.stream().collect(java.util.stream.Collectors.toMap(
                t -> t.getId(), ruleService::rulesForType)));
        return "module2_visa/admin-visa-rules";
    }

    @PostMapping("/types")
    public String addType(@RequestParam Long countryId, @RequestParam String name, @RequestParam(required = false) String category,
                          @RequestParam(required = false) BigDecimal fee, @RequestParam(required = false) String currency,
                          @RequestParam(required = false) Integer processingDays, RedirectAttributes flash) {
        FlashMessages.run(flash, "Visa type added.", () -> ruleService.addVisaType(countryId, name, category, fee, currency, processingDays));
        return "redirect:/admin/visa-rules";
    }

    @PostMapping("/types/{id}")
    public String updateType(@PathVariable Long id, @RequestParam(required = false) BigDecimal fee,
                             @RequestParam(required = false) Integer processingDays,
                             @RequestParam(defaultValue = "false") boolean active, RedirectAttributes flash) {
        FlashMessages.run(flash, "Visa type updated.", () -> ruleService.updateVisaType(id, fee, processingDays, active));
        return "redirect:/admin/visa-rules#type-" + id;
    }

    @PostMapping("/types/{id}/documents")
    public String addDocument(@PathVariable Long id, @RequestParam String docCode, @RequestParam String label,
                              @RequestParam(required = false) String nationality, @RequestParam(required = false) String allowedFormats,
                              @RequestParam(required = false) Integer maxSizeMb, @RequestParam(required = false) Integer minWidthPx,
                              @RequestParam(required = false) Integer minHeightPx,
                              @RequestParam(defaultValue = "false") boolean mandatory, RedirectAttributes flash) {
        FlashMessages.run(flash, "Document rule added.", () -> ruleService.addRequiredDocument(id, docCode, label, nationality,
                allowedFormats, maxSizeMb, minWidthPx, minHeightPx, mandatory));
        return "redirect:/admin/visa-rules#type-" + id;
    }

    @PostMapping("/documents/{ruleId}/delete")
    public String deleteDocument(@PathVariable Long ruleId, @RequestParam Long typeId, RedirectAttributes flash) {
        FlashMessages.run(flash, "Document rule removed.", () -> ruleService.deleteRequiredDocument(ruleId));
        return "redirect:/admin/visa-rules#type-" + typeId;
    }
}
