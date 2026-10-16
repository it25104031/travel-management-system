package com.travel.module4_logistics.repository;

import com.travel.module4_logistics.entity.SupplierPayment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Integer> {
    List<SupplierPayment> findByDeparture_Id(Integer departureId);

    @EntityGraph(attributePaths = {"supplier","departure"})
    List<SupplierPayment> findAllByOrderByPaymentDateDesc();
}
