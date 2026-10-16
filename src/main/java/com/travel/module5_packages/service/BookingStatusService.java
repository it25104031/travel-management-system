package com.travel.module5_packages.service;

import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.service.InvoiceService;
import com.travel.module3_billing.service.PaymentService;
import com.travel.module3_billing.service.PaymentService.InvoiceSummary;
import com.travel.module3_billing.service.PaymentSlipService;
import com.travel.module5_packages.entity.TourBooking;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookingStatusService {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final PaymentSlipService slipService;

    public BookingStatusService(InvoiceService invoiceService, PaymentService paymentService, PaymentSlipService slipService) {
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.slipService = slipService;
    }

    public record BookingView(TourBooking booking, Invoice invoice, String state, String label, String badge,
                              BigDecimal paid, BigDecimal outstanding) {
        public boolean isPayable() {
            return invoice != null && !booking.isCancelled() && outstanding != null && outstanding.signum() > 0
                    && !"REVIEW".equals(state);
        }
    }

    public record InvoiceView(Invoice invoice, String state, String label, String badge,
                              BigDecimal paid, BigDecimal outstanding, boolean slipPending) {
        public boolean isPayable() { return outstanding != null && outstanding.signum() > 0 && !slipPending; }
    }

    @Transactional(readOnly = true)
    public InvoiceView invoiceView(Invoice invoice) {
        InvoiceSummary s = paymentService.getSummary(invoice);
        boolean pending = slipService.hasPendingSlip(invoice.getInvoiceId());
        String state = pending && !"PAID".equals(s.status()) ? "REVIEW" : s.status();
        return new InvoiceView(invoice, state, label(state), badge(state), s.paidAmount(), s.outstandingBalance(), pending);
    }

    @Transactional(readOnly = true)
    public List<BookingView> views(List<TourBooking> bookings) {
        Map<Integer, InvoiceView> byInvoice = new LinkedHashMap<>();
        List<BookingView> out = new ArrayList<>();
        for (TourBooking b : bookings) {
            InvoiceView iv = null;
            if (b.getInvoiceId() != null) {
                iv = byInvoice.computeIfAbsent(b.getInvoiceId(),
                        id -> invoiceService.getInvoiceById(id).map(this::invoiceView).orElse(null));
            }
            if (b.isCancelled()) {
                out.add(new BookingView(b, iv == null ? null : iv.invoice(), "CANCELLED", "Cancelled", "badge-gray",
                        iv == null ? null : iv.paid(), iv == null ? null : iv.outstanding()));
            } else if (iv == null) {
                out.add(new BookingView(b, null, "NO_INVOICE", "Invoice removed", "badge-gray", null, null));
            } else {
                out.add(new BookingView(b, iv.invoice(), iv.state(), iv.label(), iv.badge(), iv.paid(), iv.outstanding()));
            }
        }
        return out;
    }

    static String label(String state) {
        return switch (state) {
            case "PAID" -> "Confirmed";
            case "PARTIAL" -> "Part paid";
            case "REVIEW" -> "Payment in review";
            default -> "Awaiting payment";
        };
    }

    static String badge(String state) {
        return switch (state) {
            case "PAID" -> "badge-green";
            case "PARTIAL" -> "badge-blue";
            case "REVIEW" -> "badge-yellow";
            default -> "badge-amber";
        };
    }
}
