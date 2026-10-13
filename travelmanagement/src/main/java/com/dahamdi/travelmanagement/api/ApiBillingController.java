package com.dahamdi.travelmanagement.api;

import com.dahamdi.travelmanagement.api.ApiDtos.GatewayRequest;
import com.dahamdi.travelmanagement.api.ApiDtos.InvoiceDto;
import com.dahamdi.travelmanagement.api.ApiDtos.InvoiceRequest;
import com.dahamdi.travelmanagement.api.ApiDtos.PaymentDto;
import com.dahamdi.travelmanagement.api.ApiDtos.PaymentRequest;
import com.dahamdi.travelmanagement.entity.Invoice;
import com.dahamdi.travelmanagement.repository.PaymentSlipRepository;
import com.dahamdi.travelmanagement.service.GatewayPaymentService;
import com.dahamdi.travelmanagement.service.GatewayPaymentService.GatewayResult;
import com.dahamdi.travelmanagement.service.InvoiceService;
import com.dahamdi.travelmanagement.service.PaymentService;
import com.dahamdi.travelmanagement.service.PaymentService.InvoiceSummary;
import com.dahamdi.travelmanagement.service.UserService;
import com.dahamdi.travelmanagement.util.DateUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin billing API for the Vite frontend (ADMIN only - checked by AuthInterceptor).
 * Every write goes through the same services as the Thymeleaf pages, so all
 * validation rules (amount > 0, no overpaying, unique invoice numbers ...) apply.
 */
@RestController
@RequestMapping("/api/billing")
public class ApiBillingController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final GatewayPaymentService gatewayPaymentService;
    private final UserService userService;
    private final PaymentSlipRepository paymentSlipRepository;

    public ApiBillingController(InvoiceService invoiceService, PaymentService paymentService,
                                GatewayPaymentService gatewayPaymentService, UserService userService,
                                PaymentSlipRepository paymentSlipRepository) {
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.gatewayPaymentService = gatewayPaymentService;
        this.userService = userService;
        this.paymentSlipRepository = paymentSlipRepository;
    }

    // ---------------- dashboard ----------------

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
        result.put("totalUsers", userService.countUsers());
        result.put("pendingSlips", paymentSlipRepository.countByStatus("PENDING"));
        result.put("topOpenInvoices", open.stream().limit(5).toList());
        return result;
    }

    // ---------------- invoices ----------------

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

    // ---------------- payments ----------------

    @GetMapping("/payments")
    public List<PaymentDto> payments() {
        return paymentService.getAllPayments().stream()
                .sorted(Comparator.comparing(com.dahamdi.travelmanagement.entity.Payment::getPaymentDate).reversed())
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

    // ---------------- gateway ----------------

    @PostMapping("/gateway")
    public Map<String, Object> gatewayPay(@RequestBody GatewayRequest body) {
        GatewayResult result = gatewayPaymentService.pay(body.invoiceId(), body.customerName(),
                body.amount(), body.paymentMethod());
        Invoice invoice = result.invoice();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("invoice", InvoiceDto.of(invoice, paymentService.getSummary(invoice)));
        response.put("payment", PaymentDto.of(result.payment()));
        response.put("autoGenerated", result.autoGenerated());
        return response;
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
