package com.dahamdi.travelmanagement.controller;

import com.dahamdi.travelmanagement.config.AuthInterceptor;
import com.dahamdi.travelmanagement.entity.Invoice;
import com.dahamdi.travelmanagement.entity.User;
import com.dahamdi.travelmanagement.service.InvoiceService;
import com.dahamdi.travelmanagement.service.PaymentService;
import com.dahamdi.travelmanagement.service.PaymentService.InvoiceSummary;
import com.dahamdi.travelmanagement.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    public AuthController(
            UserService userService,
            InvoiceService invoiceService,
            PaymentService paymentService) {
        this.userService = userService;
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
    }

    @GetMapping("/login")
    public String showLoginPage(HttpSession session) {
        if (session.getAttribute(AuthInterceptor.SESSION_USER) != null) {
            return "redirect:/";
        }
        return "login";
    }

    @GetMapping("/")
    public String rootRedirect(HttpSession session) {
        User loggedInUser = (User) session.getAttribute(AuthInterceptor.SESSION_USER);
        if (loggedInUser == null) {
            return "redirect:/login";
        }
        if ("ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/user/dashboard";
    }

    @PostMapping("/login")
    public String processLogin(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request,
            Model model) {

        Optional<User> userOpt = userService.authenticate(username, password);
        if (userOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Invalid username or password. Please try again.");
            return "login";
        }

        User user = userOpt.get();

        // New session id after login (prevents session fixation)
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        // Store a copy WITHOUT the password in the session
        session.setAttribute(AuthInterceptor.SESSION_USER, UserService.toSessionUser(user));

        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/user/dashboard";
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam String email,
            Model model) {

        try {
            userService.register(username, password, fullName, email);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }

        model.addAttribute("successMessage", "Account created successfully! Please log in.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // Access to /admin/** is checked by AuthInterceptor (ADMIN only)
    @GetMapping("/admin/dashboard")
    public String showAdminDashboard(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute(AuthInterceptor.SESSION_USER);

        BigDecimal totalRevenue = paymentService.getTotalRevenue();

        model.addAttribute("user", loggedInUser);
        model.addAttribute("totalInvoices", invoiceService.countInvoices());
        model.addAttribute("totalPayments", paymentService.countPayments());
        model.addAttribute("totalRevenue", PaymentService.formatMoney(totalRevenue));
        model.addAttribute("totalUsers", userService.countUsers());

        return "admin-dashboard";
    }

    @GetMapping("/user/dashboard")
    public String showUserDashboard(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute(AuthInterceptor.SESSION_USER);
        model.addAttribute("user", loggedInUser);
        return "user-dashboard";
    }

    @GetMapping("/payment-gateway")
    public String showPaymentGateway(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute(AuthInterceptor.SESSION_USER);

        if ("ADMIN".equals(loggedInUser.getRole())) {
            List<Invoice> allInvoices = invoiceService.getAllInvoices();
            Map<Integer, InvoiceSummary> summaries = paymentService.getSummaries(allInvoices);

            List<Invoice> unpaidOrPartialInvoices = new ArrayList<>();
            Map<Integer, BigDecimal> paidAmounts = new HashMap<>();
            Map<Integer, BigDecimal> outstandingBalances = new HashMap<>();
            Map<Integer, String> calculatedStatuses = new HashMap<>();

            for (Invoice inv : allInvoices) {
                InvoiceSummary s = summaries.get(inv.getInvoiceId());
                if (!"PAID".equals(s.status()) && s.outstandingBalance().signum() > 0) {
                    unpaidOrPartialInvoices.add(inv);
                    paidAmounts.put(inv.getInvoiceId(), s.paidAmount());
                    outstandingBalances.put(inv.getInvoiceId(), s.outstandingBalance());
                    calculatedStatuses.put(inv.getInvoiceId(), s.status());
                }
            }

            model.addAttribute("invoices", unpaidOrPartialInvoices);
            model.addAttribute("paidAmounts", paidAmounts);
            model.addAttribute("outstandingBalances", outstandingBalances);
            model.addAttribute("calculatedStatuses", calculatedStatuses);
        }

        model.addAttribute("user", loggedInUser);
        return "payment-gateway";
    }
}
