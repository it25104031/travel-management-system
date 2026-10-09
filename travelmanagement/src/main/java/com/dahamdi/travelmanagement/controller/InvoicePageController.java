package com.dahamdi.travelmanagement.controller;

import com.dahamdi.travelmanagement.entity.Invoice;
import com.dahamdi.travelmanagement.exception.NotFoundException;
import com.dahamdi.travelmanagement.service.InvoiceService;
import com.dahamdi.travelmanagement.service.PaymentService;
import com.dahamdi.travelmanagement.service.PaymentService.InvoiceSummary;
import com.dahamdi.travelmanagement.util.DateUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

// Access to /invoices/** is checked by AuthInterceptor (ADMIN only)
@Controller
public class InvoicePageController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    public InvoicePageController(InvoiceService invoiceService, PaymentService paymentService) {
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
    }

    @GetMapping("/invoices")
    public String showInvoicesPage(Model model) {
        List<Invoice> invoices = invoiceService.getAllInvoices();
        model.addAttribute("invoices", invoices);
        InvoiceViewHelper.addInvoiceSummaries(model, invoices, paymentService);
        return "invoices";
    }

    @PostMapping("/invoices/add")
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
        return "redirect:/invoices";
    }

    @GetMapping("/invoices/edit/{id}")
    public String showEditInvoicePage(@PathVariable Integer id, Model model) {
        Invoice invoice = invoiceService.getInvoiceOrThrow(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("summary", paymentService.getSummary(invoice));
        return "edit-invoice";
    }

    @PostMapping("/invoices/update/{id}")
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
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/invoices/edit/" + id;
        }
        return "redirect:/invoices";
    }

    @GetMapping("/invoices/print/{id}")
    public String printInvoice(@PathVariable Integer id, Model model) {
        Invoice invoice = invoiceService.getInvoiceOrThrow(id);
        InvoiceSummary summary = paymentService.getSummary(invoice);

        model.addAttribute("invoice", invoice);
        model.addAttribute("paidAmount", summary.paidAmount());
        model.addAttribute("outstandingBalance", summary.outstandingBalance());
        model.addAttribute("calculatedStatus", summary.status());

        return "print-invoice";
    }

    @PostMapping("/invoices/delete/{id}")
    public String deleteInvoice(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.deleteInvoice(id);
        } catch (IllegalStateException | NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/invoices";
    }
}
