package com.travel.module2_visa.controller;

import com.travel.common.FlashMessages;
import com.travel.module2_visa.service.VisaRuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/countries")
public class AdminCountryController {

    private final VisaRuleService ruleService;

    public AdminCountryController(VisaRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public String listCountries(Model model) {
        model.addAttribute("countries", ruleService.allCountries());
        return "module2_visa/admin-countries";
    }

    @PostMapping("/add")
    public String addCountry(@RequestParam String countryName,
                             @RequestParam String countryCode,
                             @RequestParam String formRoute,
                             RedirectAttributes flash) {
        FlashMessages.run(flash, "Country added.", () -> ruleService.addCountry(countryName, countryCode, formRoute));
        return "redirect:/admin/countries";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes flash) {
        FlashMessages.run(flash, "Country availability updated.", () -> ruleService.toggleCountry(id));
        return "redirect:/admin/countries";
    }
}
