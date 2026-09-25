package com.dahamdi.travelmanagement.controller;

import com.dahamdi.travelmanagement.entity.Invoice;
import com.dahamdi.travelmanagement.service.InvoiceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }
    @PutMapping("/{id}")
    public Invoice updateInvoice(
            @PathVariable Integer id,
            @RequestBody Invoice invoice) {

        Invoice existingInvoice = invoiceService.getInvoiceById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        existingInvoice.setInvoiceNumber(invoice.getInvoiceNumber());
        existingInvoice.setCustomerName(invoice.getCustomerName());
        existingInvoice.setInvoiceDate(invoice.getInvoiceDate());
        existingInvoice.setTotalAmount(invoice.getTotalAmount());
        existingInvoice.setInvoiceStatus(invoice.getInvoiceStatus());

        return invoiceService.createInvoice(existingInvoice);
    }

    @GetMapping
    public List<Invoice> getAllInvoices() {
        return invoiceService.getAllInvoices();
    }

    @GetMapping("/{id}")
    public Invoice getInvoiceById(@PathVariable Integer id) {
        return invoiceService.getInvoiceById(id).orElse(null);
    }

    @PostMapping
    public Invoice createInvoice(@RequestBody Invoice invoice) {
        return invoiceService.createInvoice(invoice);
    }

    @DeleteMapping("/{id}")
    public void deleteInvoice(@PathVariable Integer id) {
        invoiceService.deleteInvoice(id);
    }
}

