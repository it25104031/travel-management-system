package com.dahamdi.travelmanagement.repository;

import com.dahamdi.travelmanagement.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
}