package com.travel.module3_billing.api;

import com.travel.module3_billing.api.ApiDtos.InvoiceDto;
import com.travel.module3_billing.api.ApiDtos.InvoiceRequest;
import com.travel.module3_billing.api.ApiDtos.PaymentDto;
import com.travel.module3_billing.api.ApiDtos.PaymentRequest;
import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.repository.PaymentSlipRepository;
import com.travel.module3_billing.service.InvoiceService;
import com.travel.module3_billing.service.PaymentService;
import com.travel.module3_billing.service.PaymentService.InvoiceSummary;
import com.travel.module1_auth.repository.AdminUserRepository;
import com.travel.module3_billing.util.DateUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class ApiBillingController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final AdminUserRepository adminUserRepository;
    private final PaymentSlipRepository paymentSlipRepository;

    public ApiBillingController(InvoiceService invoiceService, PaymentService paymentService,
                                AdminUserRepository adminUserRepository,
                                PaymentSlipRepository paymentSlipRepository) {
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.adminUserRepository = adminUserRepository;
        this.paymentSlipRepository = paymentSlipRepository;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        List<InvoiceDto> invoices = allInvoices();

        List<InvoiceDto> open = invoices.stream()
                .filter(i -> !"PAID".equals(i.status()))
                .sorted(Comparator.comparing(InvoiceDto::outstandingBalance).reversed())
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalInvoices", invoices.size());
        result.put("openInvoices", open.size());
        result.put("totalPayments", paymentService.countPayments());
        result.put("totalRevenue", paymentService.getTotalRevenue());
        result.put("totalOutstanding", open.stream()
                .map(InvoiceDto::outstandingBalance)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        result.put("totalUsers", adminUserRepository.count());
        result.put("pendingSlips", paymentSlipRepository.countByStatus("PENDING"));
        result.put("topOpenInvoices", open.stream().limit(5).toList());
        return result;
    }

    @GetMapping("/invoices")
    public List<InvoiceDto> invoices() {
        return allInvoices();
    }

    @GetMapping("/invoices/{id}")
    public InvoiceDto invoice(@PathVariable Integer id) {
        Invoice invoice = invoiceService.getInvoiceOrThrow(id);
        return InvoiceDto.of(invoice, paymentService.getSummary(invoice));
    }

    @PostMapping("/invoices")
    public ResponseEntity<InvoiceDto> createInvoice(@RequestBody InvoiceRequest body) {
        Invoice created = invoiceService.createInvoice(body.invoiceNumber(), body.customerName(),
                DateUtils.parseDateTime(body.invoiceDate()), body.totalAmount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InvoiceDto.of(created, paymentService.getSummary(created)));
    }

    @PutMapping("/invoices/{id}")
    public InvoiceDto updateInvoice(@PathVariable Integer id, @RequestBody InvoiceRequest body) {
        Invoice updated = invoiceService.updateInvoice(id, body.invoiceNumber(), body.customerName(),
                DateUtils.parseDateTime(body.invoiceDate()), body.totalAmount());
        return InvoiceDto.of(updated, paymentService.getSummary(updated));
    }

    @DeleteMapping("/invoices/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Integer id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/payments")
    public List<PaymentDto> payments() {
        return paymentService.getAllPayments().stream()
                .sorted(Comparator.comparing(com.travel.module3_billing.entity.Payment::getPaymentDate).reversed())
                .map(PaymentDto::of)
                .toList();
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentDto> createPayment(@RequestBody PaymentRequest body) {
        var payment = paymentService.createPayment(body.invoiceId(), DateUtils.parseDateTime(body.paymentDate()),
                body.amount(), body.paymentMethod(), body.paymentStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentDto.of(payment));
    }

    @PutMapping("/payments/{id}")
    public PaymentDto updatePayment(@PathVariable Integer id, @RequestBody PaymentRequest body) {
        var payment = paymentService.updatePayment(id, body.invoiceId(), DateUtils.parseDateTime(body.paymentDate()),
                body.amount(), body.paymentMethod(), body.paymentStatus());
        return PaymentDto.of(payment);
    }

    @DeleteMapping("/payments/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Integer id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }

    private List<InvoiceDto> allInvoices() {
        List<Invoice> invoices = invoiceService.getAllInvoices();
        Map<Integer, InvoiceSummary> summaries = paymentService.getSummaries(invoices);
        return invoices.stream()
                .sorted(Comparator.comparing(Invoice::getInvoiceDate).reversed())
                .map(i -> InvoiceDto.of(i, summaries.get(i.getInvoiceId())))
                .toList();
    }
}
