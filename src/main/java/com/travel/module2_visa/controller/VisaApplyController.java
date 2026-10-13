package com.travel.module2_visa.controller;

import com.travel.common.ValidationException;
import com.travel.module1_auth.service.SiteCustomerService;
import com.travel.module2_visa.domain.CountryEntity;
import com.travel.module2_visa.domain.CountryForm;
import com.travel.module2_visa.domain.IndiaVisaApplicationEntity;
import com.travel.module2_visa.domain.VisaApplicationEntity;
import com.travel.module2_visa.domain.VisaTypeEntity;
import com.travel.module2_visa.service.VisaApplicationService;
import com.travel.module2_visa.service.VisaRuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Application flow for applicants. Travellers must be signed in to their website account
 * (the application then shows in their profile); staff can also enter one for a walk-in customer.
 */
@Controller
@RequestMapping("/visas")
public class VisaApplyController {

    private final VisaApplicationService applicationService;
    private final VisaRuleService ruleService;
    private final SiteCustomerService customerService;

    public VisaApplyController(VisaApplicationService applicationService, VisaRuleService ruleService,
                               SiteCustomerService customerService) {
        this.applicationService = applicationService;
        this.ruleService = ruleService;
        this.customerService = customerService;
    }

    // Applicants must not be able to set workflow fields by adding them to the form post
    @InitBinder("visaApplication")
    public void restrictFields(WebDataBinder binder) {
        binder.setDisallowedFields("id", "status", "createdAt", "documents*", "govReferenceNo", "govSubmissionDate",
                "grantDate", "visaValidFrom", "visaValidTo", "entryType", "approvedVisaPath", "assignedOperator*",
                "departure*", "passportInvalidated", "siteCustomerId");
    }

    // The posted form is bound onto the entity type of the destination in the URL (India, Sri Lanka, ...)
    @ModelAttribute("visaApplication")
    public VisaApplicationEntity visaApplication(@PathVariable(required = false) String slug) {
        return CountryForm.bySlug(slug == null ? "" : slug).orElse(CountryForm.INDIA).newApplication();
    }

    // Redirect direct /visas/apply links to the country selector
    @GetMapping("/apply")
    public String redirectToCountrySelector() {
        return "redirect:/visas/select-country";
    }

    // 1. Customer Country Selection Gateway
    @GetMapping("/select-country")
    public String showCountrySelector(Model model) {
        model.addAttribute("countries", ruleService.activeCountries());
        return "module2_visa/visa-country-select";
    }

    // 2. Choose visa type + nationality, then render that country's form with the matching document slots
    @GetMapping("/apply/{slug}")
    public String showApplicationForm(@PathVariable String slug,
                                      @RequestParam(required = false) Long visaTypeId,
                                      @RequestParam(required = false) String nationality,
                                      Model model) {
        CountryForm form = CountryForm.bySlug(slug).orElse(null);
        CountryEntity country = form == null ? null : ruleService.country(form.getCountryCode()).orElse(null);
        if (country == null || !Boolean.TRUE.equals(country.getIsActive())) {
            return "redirect:/visas/select-country";
        }
        VisaTypeEntity type = ruleService.visaType(visaTypeId)
                .filter(t -> Boolean.TRUE.equals(t.getActive()) && t.getCountry().getId().equals(country.getId()))
                .orElse(null);
        if (type == null || nationality == null || nationality.isBlank()) {
            model.addAttribute("country", country);
            model.addAttribute("form", form);
            model.addAttribute("visaTypes", ruleService.activeVisaTypes(country));
            return "module2_visa/visa-apply-start";
        }
        VisaApplicationEntity app = form.newApplication();
        app.setVisaTypeId(type.getId());
        if (app instanceof IndiaVisaApplicationEntity india) india.setVisaService(type.getName());
        app.setNationality(nationality.trim());
        customerService.current().ifPresent(c -> app.setEmailId(c.getEmail()));
        populateForm(model, app, type, form);
        return form.getTemplate();
    }

    // 3. Submission (form data + one file per required document)
    @PostMapping("/apply/{slug}")
    public String submitApplication(@PathVariable String slug,
                                    @ModelAttribute("visaApplication") VisaApplicationEntity application,
                                    MultipartHttpServletRequest request,
                                    Model model, RedirectAttributes flash) {
        CountryForm form = CountryForm.bySlug(slug).orElse(null);
        if (form == null) return "redirect:/visas/select-country";
        customerService.current().ifPresent(c -> application.setSiteCustomerId(c.getId()));
        try {
            VisaApplicationService.SubmissionResult result = applicationService.submit(application, request.getFileMap());
            flash.addFlashAttribute("credentials", result.credentials());
            flash.addFlashAttribute("applicantName", result.application().getFullName());
            flash.addFlashAttribute("applicantEmail", result.application().getEmailId());
            return "redirect:/visas/apply/submitted";
        } catch (ValidationException e) {
            VisaTypeEntity type = ruleService.visaType(application.getVisaTypeId()).orElse(null);
            if (type == null) return "redirect:" + form.getRoute();
            model.addAttribute("errors", e.getErrors());
            populateForm(model, application, type, form);
            return form.getTemplate();
        }
    }

    @GetMapping("/apply/submitted")
    public String submitted(Model model) {
        // Credentials are only available on the redirect straight after submission (flash attribute)
        if (!model.containsAttribute("credentials")) return "redirect:/portal/login";
        return "module2_visa/visa-submitted";
    }

    private void populateForm(Model model, VisaApplicationEntity app, VisaTypeEntity type, CountryForm form) {
        model.addAttribute("form", form);
        model.addAttribute("visaApplication", app);
        model.addAttribute("visaType", type);
        model.addAttribute("requiredDocuments", ruleService.requiredDocuments(type.getId(), app.getNationality()));
    }
}
