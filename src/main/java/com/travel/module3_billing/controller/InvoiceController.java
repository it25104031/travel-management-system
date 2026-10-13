package com.travel.module3_billing.controller;

import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.service.InvoiceService;
import com.travel.module3_billing.service.PaymentService;
import com.travel.module3_billing.service.PaymentService.InvoiceSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for invoices. staff login required (see SecurityConfig).
 * Uses the same InvoiceService rules as the web pages. Errors come back as
 * {"error": "..."} (see ApiExceptionHandler). The invoiceStatus in a request body
 * is ignored - it is always calculated from the payments.
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    public InvoiceController(InvoiceService invoiceService, PaymentService paymentService) {
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<Invoice> getAllInvoices() {
        return invoiceService.getAllInvoices();
    }

    @GetMapping("/{id}")
    public Invoice getInvoiceById(@PathVariable Integer id) {
        return invoiceService.getInvoiceOrThrow(id);
    }

    @GetMapping("/{id}/summary")
    public Map<String, Object> getInvoiceSummary(@PathVariable Integer id) {
        Invoice invoice = invoiceService.getInvoiceOrThrow(id);
        InvoiceSummary summary = paymentService.getSummary(invoice);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("invoiceId", invoice.getInvoiceId());
        result.put("invoiceNumber", invoice.getInvoiceNumber());
        result.put("customerName", invoice.getCustomerName());
        result.put("totalAmount", invoice.getTotalAmount());
        result.put("paidAmount", summary.paidAmount());
        result.put("outstandingBalance", summary.outstandingBalance());
        result.put("invoiceStatus", summary.status());
        return result;
    }

    @PostMapping
    public ResponseEntity<Invoice> createInvoice(@RequestBody Invoice invoice) {
        Invoice created = invoiceService.createInvoice(
                invoice.getInvoiceNumber(),
                invoice.getCustomerName(),
                invoice.getInvoiceDate(),
                invoice.getTotalAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Invoice updateInvoice(@PathVariable Integer id, @RequestBody Invoice invoice) {
        return invoiceService.updateInvoice(
                id,
                invoice.getInvoiceNumber(),
                invoice.getCustomerName(),
                invoice.getInvoiceDate(),
                invoice.getTotalAmount());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Integer id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
