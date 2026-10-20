package com.travel.module3_billing.controller;

import com.travel.module3_billing.entity.Payment;
import com.travel.module3_billing.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{id}")
    public Payment getPaymentById(@PathVariable Integer id) {
        return paymentService.getPaymentOrThrow(id);
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody Payment payment) {
        Payment created = paymentService.createPayment(
                invoiceIdOf(payment),
                payment.getPaymentDate(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Payment updatePayment(@PathVariable Integer id, @RequestBody Payment payment) {
        return paymentService.updatePayment(
                id,
                invoiceIdOf(payment),
                payment.getPaymentDate(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Integer id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invoice/{invoiceId}/outstanding-balance")
    public BigDecimal getOutstandingBalance(@PathVariable Integer invoiceId) {
        return paymentService.getOutstandingBalance(invoiceId);
    }

    @GetMapping("/invoice/{invoiceId}/completed-total")
    public BigDecimal getCompletedPaymentTotal(@PathVariable Integer invoiceId) {
        return paymentService.getCompletedPaymentTotal(invoiceId);
    }

    @GetMapping("/invoice/{invoiceId}/details")
    public Map<String, Object> getInvoicePaymentDetails(@PathVariable Integer invoiceId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("invoiceId", invoiceId);
        map.put("completedTotal", paymentService.getCompletedPaymentTotal(invoiceId));
        map.put("outstandingBalance", paymentService.getOutstandingBalance(invoiceId));
        map.put("status", paymentService.getInvoiceStatus(invoiceId));
        return map;
    }

    private static Integer invoiceIdOf(Payment payment) {
        if (payment.getInvoice() == null || payment.getInvoice().getInvoiceId() == null) {
            throw new IllegalArgumentException("invoice.invoiceId is required.");
        }
        return payment.getInvoice().getInvoiceId();
    }
}
