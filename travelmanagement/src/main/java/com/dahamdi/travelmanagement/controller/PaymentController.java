package com.dahamdi.travelmanagement.controller;

import com.dahamdi.travelmanagement.entity.Payment;
import com.dahamdi.travelmanagement.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PutMapping("/{id}")
    public Payment updatePayment(
            @PathVariable Integer id,
            @RequestBody Payment payment) {

        Payment existingPayment = paymentService.getPaymentById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        existingPayment.setInvoice(payment.getInvoice());
        existingPayment.setPaymentDate(payment.getPaymentDate());
        existingPayment.setAmount(payment.getAmount());
        existingPayment.setPaymentMethod(payment.getPaymentMethod());
        existingPayment.setPaymentStatus(payment.getPaymentStatus());

        return paymentService.createPayment(existingPayment);
    }

    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{id}")
    public Payment getPaymentById(@PathVariable Integer id) {
        return paymentService.getPaymentById(id).orElse(null);
    }

    @PostMapping
    public Payment createPayment(@RequestBody Payment payment) {
        return paymentService.createPayment(payment);
    }

    @DeleteMapping("/{id}")
    public void deletePayment(@PathVariable Integer id) {
        paymentService.deletePayment(id);
    }
}