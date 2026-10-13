package com.travel.module4_logistics.repository;

import com.travel.module4_logistics.entity.Transport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface TransportRepository extends JpaRepository<Transport, Integer> {
    boolean existsBySupplier_IdAndVehicleReferenceIgnoreCaseAndServiceDate(Integer supplierId, String reference, LocalDate date);
    List<Transport> findByDeparture_Id(Integer departureId);
    // Load the linked records with the list (open-in-view is off in the combined app)
    @EntityGraph(attributePaths = {"supplier","departure"})
    List<Transport> findAllByOrderByServiceDateDesc();
}
