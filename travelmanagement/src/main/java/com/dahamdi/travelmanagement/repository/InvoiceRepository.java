package com.dahamdi.travelmanagement.repository;

import com.dahamdi.travelmanagement.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    // Used when editing: is this number already used by a DIFFERENT invoice?
    boolean existsByInvoiceNumberAndInvoiceIdNot(String invoiceNumber, Integer invoiceId);
}
