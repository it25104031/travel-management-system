package com.travel.module3_billing.repository;

import com.travel.module3_billing.entity.PaymentSlip;
import com.travel.module1_auth.domain.AdminUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentSlipRepository extends JpaRepository<PaymentSlip, Integer> {

    List<PaymentSlip> findByUserOrderByCreatedAtDesc(AdminUserEntity user);

    List<PaymentSlip> findAllByOrderByCreatedAtDesc();

    List<PaymentSlip> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    List<PaymentSlip> findByInvoice_InvoiceIdOrderByCreatedAtDesc(Integer invoiceId);

    boolean existsByInvoice_InvoiceIdAndStatus(Integer invoiceId, String status);

    List<PaymentSlip> findByStatusOrderByCreatedAtDesc(String status);

    boolean existsByInvoice_InvoiceId(Integer invoiceId);

    long countByStatus(String status);
}
