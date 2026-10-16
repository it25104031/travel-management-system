package com.travel.module3_billing.controller;

import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.service.PaymentService;
import com.travel.module3_billing.service.PaymentService.InvoiceSummary;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
