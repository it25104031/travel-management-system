package com.dahamdi.travelmanagement.repository;

import com.dahamdi.travelmanagement.entity.PaymentSlip;
import com.dahamdi.travelmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentSlipRepository extends JpaRepository<PaymentSlip, Integer> {

    List<PaymentSlip> findByUserOrderByCreatedAtDesc(User user);

    List<PaymentSlip> findAllByOrderByCreatedAtDesc();

    List<PaymentSlip> findByStatusOrderByCreatedAtDesc(String status);

    boolean existsByInvoice_InvoiceId(Integer invoiceId);

    long countByStatus(String status);
}
