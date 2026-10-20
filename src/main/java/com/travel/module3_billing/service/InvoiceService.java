package com.travel.module3_billing.service;

import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.exception.NotFoundException;
import com.travel.module3_billing.repository.InvoiceRepository;
import com.travel.module3_billing.repository.PaymentRepository;
import com.travel.module3_billing.repository.PaymentSlipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class InvoiceService {

    private static final DateTimeFormatter NUMBER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentSlipRepository paymentSlipRepository;
    private final PaymentService paymentService;

    private final org.springframework.beans.factory.ObjectProvider<InvoiceDeletionGuard> deletionGuards;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          PaymentRepository paymentRepository,
                          PaymentSlipRepository paymentSlipRepository,
                          PaymentService paymentService,
                          org.springframework.beans.factory.ObjectProvider<InvoiceDeletionGuard> deletionGuards) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.paymentSlipRepository = paymentSlipRepository;
        this.paymentService = paymentService;
        this.deletionGuards = deletionGuards;
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public Optional<Invoice> getInvoiceById(Integer id) {
        return invoiceRepository.findById(id);
    }

    public Invoice getInvoiceOrThrow(Integer id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invoice #" + id + " was not found."));
    }

    public long countInvoices() {
        return invoiceRepository.count();
    }

    @Transactional
    public Invoice createInvoice(String invoiceNumber, String customerName,
                                 LocalDateTime invoiceDate, BigDecimal totalAmount) {

        String number = requireText(invoiceNumber, "Invoice number");
        String customer = requireText(customerName, "Customer name");
        validateDateAndTotal(invoiceDate, totalAmount);

        if (number.length() > 30) {
            throw new IllegalArgumentException("Invoice number can be at most 30 characters.");
        }
        if (invoiceRepository.existsByInvoiceNumber(number)) {
            throw new IllegalArgumentException("Invoice number '" + number + "' already exists.");
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(number);
        invoice.setCustomerName(customer);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setTotalAmount(totalAmount);
        invoice.setInvoiceStatus(PaymentService.calculateStatus(totalAmount, BigDecimal.ZERO));

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice updateInvoice(Integer id, String invoiceNumber, String customerName,
                                 LocalDateTime invoiceDate, BigDecimal totalAmount) {

        Invoice invoice = getInvoiceOrThrow(id);

        String number = requireText(invoiceNumber, "Invoice number");
        String customer = requireText(customerName, "Customer name");
        validateDateAndTotal(invoiceDate, totalAmount);

        if (number.length() > 30) {
            throw new IllegalArgumentException("Invoice number can be at most 30 characters.");
        }
        if (invoiceRepository.existsByInvoiceNumberAndInvoiceIdNot(number, id)) {
            throw new IllegalArgumentException("Invoice number '" + number + "' is already used by another invoice.");
        }

        BigDecimal alreadyPaid = paymentService.getCompletedPaymentTotal(id);
        if (totalAmount.compareTo(alreadyPaid) < 0) {
            throw new IllegalArgumentException(
                    "Total amount can't be less than the amount already paid (LKR "
                            + PaymentService.formatMoney(alreadyPaid) + ").");
        }

        invoice.setInvoiceNumber(number);
        invoice.setCustomerName(customer);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setTotalAmount(totalAmount);
        invoice.setInvoiceStatus(PaymentService.calculateStatus(totalAmount, alreadyPaid));

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void deleteInvoice(Integer id) {
        Invoice invoice = getInvoiceOrThrow(id);

        if (paymentRepository.existsByInvoice_InvoiceId(id)) {
            throw new IllegalStateException("Invoice " + invoice.getInvoiceNumber()
                    + " has payments recorded against it, so it can't be deleted. Delete its payments first.");
        }
        if (paymentSlipRepository.existsByInvoice_InvoiceId(id)) {
            throw new IllegalStateException("Invoice " + invoice.getInvoiceNumber()
                    + " has uploaded payment slips, so it can't be deleted.");
        }
        for (InvoiceDeletionGuard guard : deletionGuards.orderedStream().toList()) {
            String reason = guard.blockReason(id);
            if (reason != null) {
                throw new IllegalStateException("Invoice " + invoice.getInvoiceNumber() + " can't be deleted: " + reason);
            }
        }

        invoiceRepository.delete(invoice);
    }

    public String generateInvoiceNumber() {
        String number;
        do {
            number = "INV-" + LocalDateTime.now().format(NUMBER_FORMAT)
                    + "-" + ThreadLocalRandom.current().nextInt(100, 1000);
        } while (invoiceRepository.existsByInvoiceNumber(number));
        return number;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private static void validateDateAndTotal(LocalDateTime invoiceDate, BigDecimal totalAmount) {
        if (invoiceDate == null) {
            throw new IllegalArgumentException("Invoice date is required.");
        }
        if (totalAmount == null || totalAmount.signum() <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than 0.");
        }
        if (totalAmount.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException("Total amount is too large.");
        }
    }
}
