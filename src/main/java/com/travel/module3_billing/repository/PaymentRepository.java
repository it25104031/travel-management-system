package com.travel.module3_billing.repository;

import com.travel.module3_billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    @Query("""
        SELECT SUM(p.amount)
        FROM Payment p
        WHERE p.invoice.invoiceId = :invoiceId
        AND p.paymentStatus = 'COMPLETED'
    """)
    BigDecimal sumCompletedByInvoiceId(@Param("invoiceId") Integer invoiceId);

    @Query("""
        SELECT SUM(p.amount)
        FROM Payment p
        WHERE p.invoice.invoiceId = :invoiceId
        AND p.paymentStatus = 'COMPLETED'
        AND p.paymentId <> :paymentId
    """)
    BigDecimal sumOtherCompletedPayments(
            @Param("invoiceId") Integer invoiceId,
            @Param("paymentId") Integer paymentId
    );

    @Query("""
        SELECT p.invoice.invoiceId, SUM(p.amount)
        FROM Payment p
        WHERE p.paymentStatus = 'COMPLETED'
        GROUP BY p.invoice.invoiceId
    """)
    List<Object[]> sumCompletedGroupedByInvoice();

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentStatus = 'COMPLETED'")
    BigDecimal sumAllCompleted();

    boolean existsByInvoice_InvoiceId(Integer invoiceId);

    List<Payment> findByInvoice_InvoiceIdOrderByPaymentDateDesc(Integer invoiceId);
}
