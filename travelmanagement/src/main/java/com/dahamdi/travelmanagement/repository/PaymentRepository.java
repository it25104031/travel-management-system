package com.dahamdi.travelmanagement.repository;

import com.dahamdi.travelmanagement.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
}