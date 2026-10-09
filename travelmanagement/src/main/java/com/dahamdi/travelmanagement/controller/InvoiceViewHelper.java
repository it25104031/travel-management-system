package com.dahamdi.travelmanagement.controller;

import com.dahamdi.travelmanagement.entity.Invoice;
import com.dahamdi.travelmanagement.service.PaymentService;
import com.dahamdi.travelmanagement.service.PaymentService.InvoiceSummary;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Adds the paidAmounts / outstandingBalances / calculatedStatuses maps the templates use. */
final class InvoiceViewHelper {

    private InvoiceViewHelper() {
    }

    static void addInvoiceSummaries(Model model, List<Invoice> invoices, PaymentService paymentService) {
        Map<Integer, InvoiceSummary> summaries = paymentService.getSummaries(invoices);

        Map<Integer, BigDecimal> paidAmounts = new HashMap<>();
        Map<Integer, BigDecimal> outstandingBalances = new HashMap<>();
        Map<Integer, String> calculatedStatuses = new HashMap<>();

        summaries.forEach((id, s) -> {
            paidAmounts.put(id, s.paidAmount());
            outstandingBalances.put(id, s.outstandingBalance());
            calculatedStatuses.put(id, s.status());
        });

        model.addAttribute("paidAmounts", paidAmounts);
        model.addAttribute("outstandingBalances", outstandingBalances);
        model.addAttribute("calculatedStatuses", calculatedStatuses);
    }
}
