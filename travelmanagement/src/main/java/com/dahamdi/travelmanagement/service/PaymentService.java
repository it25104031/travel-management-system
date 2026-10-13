package com.dahamdi.travelmanagement.service;

import com.dahamdi.travelmanagement.entity.Invoice;
import com.dahamdi.travelmanagement.entity.Payment;
import com.dahamdi.travelmanagement.exception.NotFoundException;
import com.dahamdi.travelmanagement.repository.InvoiceRepository;
import com.dahamdi.travelmanagement.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * All payment rules live here, so the web pages, the REST API, the payment gateway
 * and slip approval all follow exactly the same rules:
 *  - amount must be greater than 0
 *  - a payment can never be more than the invoice's outstanding balance
 *  - the invoice status (UNPAID / PARTIAL / PAID) is recalculated after every change
 */
@Service
public class PaymentService {

    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final Set<String> PAYMENT_STATUSES = Set.of("COMPLETED", "PENDING", "FAILED");

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository) {

        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    /** Paid / outstanding / status of one invoice, calculated from its COMPLETED payments. */
    public record InvoiceSummary(BigDecimal paidAmount, BigDecimal outstandingBalance, String status) {
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(Integer id) {
        return paymentRepository.findById(id);
    }

    public Payment getPaymentOrThrow(Integer id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment #" + id + " was not found."));
    }

    public long countPayments() {
        return paymentRepository.count();
    }

    public BigDecimal getTotalRevenue() {
        return zeroIfNull(paymentRepository.sumAllCompleted());
    }

    public BigDecimal getCompletedPaymentTotal(Integer invoiceId) {
        return zeroIfNull(paymentRepository.sumCompletedByInvoiceId(invoiceId));
    }

    public BigDecimal getOutstandingBalance(Integer invoiceId) {
        Invoice invoice = findInvoice(invoiceId);
        return invoice.getTotalAmount().subtract(getCompletedPaymentTotal(invoiceId));
    }

    public BigDecimal getOutstandingBalanceExcludingPayment(Integer invoiceId, Integer paymentId) {
        Invoice invoice = findInvoice(invoiceId);
        BigDecimal others = zeroIfNull(paymentRepository.sumOtherCompletedPayments(invoiceId, paymentId));
        return invoice.getTotalAmount().subtract(others);
    }

    public String getInvoiceStatus(Integer invoiceId) {
        Invoice invoice = findInvoice(invoiceId);
        return calculateStatus(invoice.getTotalAmount(), getCompletedPaymentTotal(invoiceId));
    }

    public InvoiceSummary getSummary(Invoice invoice) {
        BigDecimal paid = getCompletedPaymentTotal(invoice.getInvoiceId());
        return buildSummary(invoice, paid);
    }

    /** Summaries for many invoices using a single grouped query (instead of ~5 queries per invoice). */
    public Map<Integer, InvoiceSummary> getSummaries(List<Invoice> invoices) {
        Map<Integer, BigDecimal> paidByInvoice = new HashMap<>();
        for (Object[] row : paymentRepository.sumCompletedGroupedByInvoice()) {
            paidByInvoice.put((Integer) row[0], toBigDecimal(row[1]));
        }

        Map<Integer, InvoiceSummary> summaries = new HashMap<>();
        for (Invoice invoice : invoices) {
            BigDecimal paid = paidByInvoice.getOrDefault(invoice.getInvoiceId(), BigDecimal.ZERO);
            summaries.put(invoice.getInvoiceId(), buildSummary(invoice, paid));
        }
        return summaries;
    }

    /** UNPAID when nothing is paid, PARTIAL when some is paid, PAID when fully paid. */
    public static String calculateStatus(BigDecimal totalAmount, BigDecimal paidAmount) {
        if (paidAmount.compareTo(totalAmount) >= 0) {
            return "PAID";             // also covers a 0-total invoice
        }
        if (paidAmount.signum() > 0) {
            return "PARTIAL";
        }
        return "UNPAID";
    }

    // ------------------------------------------------------------------
    // Writing (every change goes through validation + status refresh)
    // ------------------------------------------------------------------

    @Transactional
    public Payment createPayment(Integer invoiceId, LocalDateTime paymentDate, BigDecimal amount,
                                 String paymentMethod, String paymentStatus) {

        Invoice invoice = findInvoice(invoiceId);
        validateFields(paymentDate, amount, paymentMethod, paymentStatus);

        BigDecimal outstanding = getOutstandingBalance(invoiceId);
        checkNotMoreThanOutstanding(amount, outstanding);

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setPaymentDate(paymentDate);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod.trim().toUpperCase());
        payment.setPaymentStatus(paymentStatus.trim().toUpperCase());

        Payment saved = paymentRepository.save(payment);
        refreshInvoiceStatus(invoice);
        return saved;
    }

    @Transactional
    public Payment updatePayment(Integer paymentId, Integer invoiceId, LocalDateTime paymentDate,
                                 BigDecimal amount, String paymentMethod, String paymentStatus) {

        Payment payment = getPaymentOrThrow(paymentId);
        Invoice oldInvoice = payment.getInvoice();
        Invoice newInvoice = findInvoice(invoiceId);
        validateFields(paymentDate, amount, paymentMethod, paymentStatus);

        BigDecimal outstanding = getOutstandingBalanceExcludingPayment(invoiceId, paymentId);
        checkNotMoreThanOutstanding(amount, outstanding);

        payment.setInvoice(newInvoice);
        payment.setPaymentDate(paymentDate);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod.trim().toUpperCase());
        payment.setPaymentStatus(paymentStatus.trim().toUpperCase());

        Payment saved = paymentRepository.saveAndFlush(payment);

        refreshInvoiceStatus(newInvoice);
        if (oldInvoice != null && !oldInvoice.getInvoiceId().equals(newInvoice.getInvoiceId())) {
            refreshInvoiceStatus(oldInvoice); // the payment moved away from this invoice
        }
        return saved;
    }

    @Transactional
    public void deletePayment(Integer id) {
        Payment payment = getPaymentOrThrow(id);
        Invoice invoice = payment.getInvoice();

        paymentRepository.delete(payment);
        paymentRepository.flush();

        refreshInvoiceStatus(invoice);
    }

    /** Recalculates the stored invoice status from the COMPLETED payments and saves it. */
    @Transactional
    public void refreshInvoiceStatus(Invoice invoice) {
        if (invoice == null) {
            return;
        }
        BigDecimal paid = getCompletedPaymentTotal(invoice.getInvoiceId());
        invoice.setInvoiceStatus(calculateStatus(invoice.getTotalAmount(), paid));
        invoiceRepository.save(invoice);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Invoice findInvoice(Integer invoiceId) {
        if (invoiceId == null) {
            throw new IllegalArgumentException("Please select an invoice.");
        }
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice #" + invoiceId + " was not found."));
    }

    private void validateFields(LocalDateTime paymentDate, BigDecimal amount,
                                String paymentMethod, String paymentStatus) {
        if (paymentDate == null) {
            throw new IllegalArgumentException("Payment date is required.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0.");
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("Payment method is required.");
        }
        if (paymentStatus == null || !PAYMENT_STATUSES.contains(paymentStatus.trim().toUpperCase())) {
            throw new IllegalArgumentException("Payment status must be COMPLETED, PENDING or FAILED.");
        }
    }

    private void checkNotMoreThanOutstanding(BigDecimal amount, BigDecimal outstanding) {
        if (amount.compareTo(outstanding) > 0) {
            throw new IllegalArgumentException(
                    "Payment amount cannot be greater than the outstanding balance of LKR "
                            + formatMoney(outstanding) + ".");
        }
    }

    private InvoiceSummary buildSummary(Invoice invoice, BigDecimal paid) {
        BigDecimal outstanding = invoice.getTotalAmount().subtract(paid);
        return new InvoiceSummary(paid, outstanding, calculateStatus(invoice.getTotalAmount(), paid));
    }

    public static String formatMoney(BigDecimal value) {
        return String.format("%,.2f", value);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }
}
