package com.dahamdi.travelmanagement.controller;

import com.dahamdi.travelmanagement.entity.Invoice;
import com.dahamdi.travelmanagement.entity.Payment;
import com.dahamdi.travelmanagement.exception.NotFoundException;
import com.dahamdi.travelmanagement.service.GatewayPaymentService;
import com.dahamdi.travelmanagement.service.GatewayPaymentService.GatewayResult;
import com.dahamdi.travelmanagement.service.InvoiceService;
import com.dahamdi.travelmanagement.service.PaymentService;
import com.dahamdi.travelmanagement.util.DateUtils;
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

// Access to /payments/** is checked by AuthInterceptor (ADMIN only)
@Controller
public class PaymentPageController {

    private final PaymentService paymentService;
    private final InvoiceService invoiceService;
    private final GatewayPaymentService gatewayPaymentService;

    public PaymentPageController(
            PaymentService paymentService,
            InvoiceService invoiceService,
            GatewayPaymentService gatewayPaymentService) {
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;
        this.gatewayPaymentService = gatewayPaymentService;
    }

    @GetMapping("/payments")
    public String showPaymentsPage(Model model) {
        List<Payment> payments = paymentService.getAllPayments();
        List<Invoice> invoices = invoiceService.getAllInvoices();

        InvoiceViewHelper.addInvoiceSummaries(model, invoices, paymentService);
        model.addAttribute("payments", payments);
        model.addAttribute("invoices", invoices);

        return "payment";
    }

    @PostMapping("/payments/add")
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
        } catch (IllegalArgumentException | IllegalStateException | NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/payments";
    }

    @GetMapping("/payments/edit/{id}")
    public String showEditPaymentPage(@PathVariable Integer id, Model model) {
        Payment payment = paymentService.getPaymentOrThrow(id);
        List<Invoice> invoices = invoiceService.getAllInvoices();

        InvoiceViewHelper.addInvoiceSummaries(model, invoices, paymentService);
        model.addAttribute("payment", payment);
        model.addAttribute("invoices", invoices);

        return "edit-payment";
    }

    @PostMapping("/payments/update/{id}")
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
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/payments/edit/" + id;
        }
        return "redirect:/payments";
    }

    @PostMapping("/payments/delete/{id}")
    public String deletePayment(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            paymentService.deletePayment(id);
        } catch (NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/payments";
    }

    @PostMapping("/payments/gateway-pay")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processGatewayPayment(
            @RequestParam(required = false) Integer invoiceId,
            @RequestParam(required = false) String customerName,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String paymentMethod) {

        Map<String, Object> response = new HashMap<>();

        try {
            GatewayResult result = gatewayPaymentService.pay(invoiceId, customerName, amount, paymentMethod);
            Invoice invoice = result.invoice();

            response.put("success", true);
            response.put("invoiceId", invoice.getInvoiceId());
            response.put("invoiceNumber", invoice.getInvoiceNumber());
            response.put("customerName", invoice.getCustomerName());
            response.put("totalAmount", invoice.getTotalAmount());
            response.put("autoGenerated", result.autoGenerated());
            response.put("message", "Payment processed successfully!");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException | NotFoundException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
