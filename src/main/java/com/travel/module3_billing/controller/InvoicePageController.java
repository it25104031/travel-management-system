package com.travel.module3_billing.controller;

import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.exception.NotFoundException;
import com.travel.module3_billing.service.InvoiceService;
import com.travel.module3_billing.service.PaymentService;
import com.travel.module3_billing.service.PaymentService.InvoiceSummary;
import com.travel.module3_billing.util.DateUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class InvoicePageController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    public InvoicePageController(InvoiceService invoiceService, PaymentService paymentService) {
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
    }

    @GetMapping("/billing/invoices")
    public String showInvoicesPage(Model model) {
        List<Invoice> invoices = invoiceService.getAllInvoices();
        model.addAttribute("invoices", invoices);
        InvoiceViewHelper.addInvoiceSummaries(model, invoices, paymentService);
        return "module3_billing/invoices";
    }

    @PostMapping("/billing/invoices/add")
    public String addInvoice(
            @RequestParam String invoiceNumber,
            @RequestParam String customerName,
            @RequestParam String invoiceDate,
            @RequestParam BigDecimal totalAmount,
            RedirectAttributes redirectAttributes) {

        try {
            invoiceService.createInvoice(invoiceNumber, customerName,
                    DateUtils.parseDateTime(invoiceDate), totalAmount);
            redirectAttributes.addFlashAttribute("successMessage", "Invoice " + invoiceNumber.trim() + " created.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/billing/invoices";
    }

    @GetMapping("/billing/invoices/edit/{id}")
    public String showEditInvoicePage(@PathVariable Integer id, Model model) {
        Invoice invoice = invoiceService.getInvoiceOrThrow(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("summary", paymentService.getSummary(invoice));
        return "module3_billing/edit-invoice";
    }

    @PostMapping("/billing/invoices/update/{id}")
    public String updateInvoice(
            @PathVariable Integer id,
            @RequestParam String invoiceNumber,
            @RequestParam String customerName,
            @RequestParam String invoiceDate,
            @RequestParam BigDecimal totalAmount,
            RedirectAttributes redirectAttributes) {

        try {
            invoiceService.updateInvoice(id, invoiceNumber, customerName,
                    DateUtils.parseDateTime(invoiceDate), totalAmount);
            redirectAttributes.addFlashAttribute("successMessage", "Invoice " + invoiceNumber.trim() + " updated.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/billing/invoices/edit/" + id;
        }
        return "redirect:/billing/invoices";
    }

    @GetMapping("/billing/invoices/print/{id}")
    public String printInvoice(@PathVariable Integer id, Model model) {
        Invoice invoice = invoiceService.getInvoiceOrThrow(id);
        InvoiceSummary summary = paymentService.getSummary(invoice);

        model.addAttribute("invoice", invoice);
        model.addAttribute("paidAmount", summary.paidAmount());
        model.addAttribute("outstandingBalance", summary.outstandingBalance());
        model.addAttribute("calculatedStatus", summary.status());

        return "module3_billing/print-invoice";
    }

    @PostMapping("/billing/invoices/delete/{id}")
    public String deleteInvoice(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.deleteInvoice(id);
            redirectAttributes.addFlashAttribute("successMessage", "Invoice deleted.");
        } catch (IllegalStateException | NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/billing/invoices";
    }
}
