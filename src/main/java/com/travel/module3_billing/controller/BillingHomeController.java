package com.travel.module3_billing.controller;

import com.travel.module1_auth.repository.AdminUserRepository;
import com.travel.module3_billing.config.CurrentStaff;
import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.service.InvoiceService;
import com.travel.module3_billing.service.PaymentService;
import com.travel.module3_billing.service.PaymentService.InvoiceSummary;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Billing dashboard and the (coming soon) card payment page. */
@Controller
public class BillingHomeController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final AdminUserRepository adminUserRepository;
    private final CurrentStaff currentStaff;

    public BillingHomeController(InvoiceService invoiceService, PaymentService paymentService,
                                 AdminUserRepository adminUserRepository, CurrentStaff currentStaff) {
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.adminUserRepository = adminUserRepository;
        this.currentStaff = currentStaff;
    }

    @GetMapping("/billing")
    public String showBillingDashboard(Model model) {
        BigDecimal totalRevenue = paymentService.getTotalRevenue();
        model.addAttribute("user", currentStaff.view());
        model.addAttribute("totalInvoices", invoiceService.countInvoices());
        model.addAttribute("totalPayments", paymentService.countPayments());
        model.addAttribute("totalRevenue", PaymentService.formatMoney(totalRevenue));
        model.addAttribute("totalUsers", adminUserRepository.count());
        return "module3_billing/admin-dashboard";
    }

    /** Card payment gateway - not live yet (LankaPay merchant approval pending). */
    @GetMapping("/billing/pay")
    public String showPaymentGateway() {
        return "module3_billing/payment-gateway";
    }
}
