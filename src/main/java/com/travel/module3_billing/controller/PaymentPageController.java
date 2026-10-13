package com.travel.module3_billing.controller;

import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.entity.Payment;
import com.travel.module3_billing.exception.NotFoundException;
import com.travel.module3_billing.service.InvoiceService;
import com.travel.module3_billing.service.PaymentService;
import com.travel.module3_billing.util.DateUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Access to /billing/payments/** is checked by SecurityConfig (ADMIN only)
@Controller
public class PaymentPageController {

    private final PaymentService paymentService;
    private final InvoiceService invoiceService;

    public PaymentPageController(
            PaymentService paymentService,
            InvoiceService invoiceService) {
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;
    }

    @GetMapping("/billing/payments")
    public String showPaymentsPage(Model model) {
        List<Payment> payments = paymentService.getAllPayments();
        List<Invoice> invoices = invoiceService.getAllInvoices();

        InvoiceViewHelper.addInvoiceSummaries(model, invoices, paymentService);
        model.addAttribute("payments", payments);
        model.addAttribute("invoices", invoices);

        return "module3_billing/payment";
    }

    @PostMapping("/billing/payments/add")
    public String addPayment(
            @RequestParam Integer invoiceId,
            @RequestParam String paymentDate,
            @RequestParam BigDecimal amount,
            @RequestParam String paymentMethod,
            @RequestParam String paymentStatus,
            RedirectAttributes redirectAttributes) {

        try {
            paymentService.createPayment(invoiceId, DateUtils.parseDateTime(paymentDate),
                    amount, paymentMethod, paymentStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Payment recorded.");
        } catch (IllegalArgumentException | IllegalStateException | NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/billing/payments";
    }

    @GetMapping("/billing/payments/edit/{id}")
    public String showEditPaymentPage(@PathVariable Integer id, Model model) {
        Payment payment = paymentService.getPaymentOrThrow(id);
        List<Invoice> invoices = invoiceService.getAllInvoices();

        InvoiceViewHelper.addInvoiceSummaries(model, invoices, paymentService);
        model.addAttribute("payment", payment);
        model.addAttribute("invoices", invoices);

        return "module3_billing/edit-payment";
    }

    @PostMapping("/billing/payments/update/{id}")
    public String updatePayment(
            @PathVariable Integer id,
            @RequestParam Integer invoiceId,
            @RequestParam String paymentDate,
            @RequestParam BigDecimal amount,
            @RequestParam String paymentMethod,
            @RequestParam String paymentStatus,
            RedirectAttributes redirectAttributes) {

        try {
            paymentService.updatePayment(id, invoiceId, DateUtils.parseDateTime(paymentDate),
                    amount, paymentMethod, paymentStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Payment updated.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/billing/payments/edit/" + id;
        }
        return "redirect:/billing/payments";
    }

    @PostMapping("/billing/payments/delete/{id}")
    public String deletePayment(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            paymentService.deletePayment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Payment deleted.");
        } catch (NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/billing/payments";
    }
}
