package com.travel.module1_auth.controller;

import com.travel.common.RequestUtils;
import com.travel.module1_auth.service.SiteCustomerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin view of the travellers who signed up on the website. */
@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final SiteCustomerService customerService;

    public AdminCustomerController(SiteCustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("customers", customerService.findAll());
        return "module1_auth/admin-customers";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, Authentication auth, HttpServletRequest request, RedirectAttributes flash) {
        customerService.setActive(id, false, auth.getName(), RequestUtils.clientIp(request));
        flash.addFlashAttribute("success", "Traveller account deactivated. They can no longer sign in.");
        return "redirect:/admin/customers";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, Authentication auth, HttpServletRequest request, RedirectAttributes flash) {
        customerService.setActive(id, true, auth.getName(), RequestUtils.clientIp(request));
        flash.addFlashAttribute("success", "Traveller account reactivated.");
        return "redirect:/admin/customers";
    }
}
