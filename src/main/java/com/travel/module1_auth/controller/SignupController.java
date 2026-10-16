package com.travel.module1_auth.controller;

import com.travel.common.RequestUtils;
import com.travel.common.ValidationException;
import com.travel.module1_auth.domain.SiteCustomerEntity;
import com.travel.module1_auth.service.SiteCustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class SignupController {

    private final SiteCustomerService customerService;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public SignupController(SiteCustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/signup")
    public String signupPage(@RequestParam(name = "continue", required = false) String next, Model model) {
        model.addAttribute("continueTo", next);
        return "module1_auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String fullName, @RequestParam String email,
                         @RequestParam(required = false) String phone,
                         @RequestParam String password, @RequestParam String confirmPassword,
                         @RequestParam(name = "continue", required = false) String next,
                         HttpServletRequest request, HttpServletResponse response,
                         Model model, RedirectAttributes flash) {
        SiteCustomerEntity customer;
        try {
            customer = customerService.register(fullName, email, phone, password, confirmPassword, RequestUtils.clientIp(request));
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            model.addAttribute("continueTo", next);
            return "module1_auth/signup";
        }

        request.getSession(true);
        request.changeSessionId();
        var token = UsernamePasswordAuthenticationToken.authenticated(customer.getEmail(), null,
                List.of(new SimpleGrantedAuthority(SiteCustomerService.ROLE_MEMBER)));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);

        flash.addFlashAttribute("success", "Welcome, " + customer.getFirstName() + "! Your account is ready.");
        String target = com.travel.module1_auth.config.CustomLoginSuccessHandler.safeLocal(next);
        if (target == null || target.startsWith("/staff") || target.startsWith("/admin")) target = "/my";
        return "redirect:" + target;
    }
}
