package com.travel.module2_visa.repository;

import com.travel.module2_visa.domain.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<IncidentEntity, Long> {
    List<IncidentEntity> findAllByOrderByCreatedAtDesc();

    List<IncidentEntity> findByDepartureIdOrderByCreatedAtDesc(Long departureId);
}
