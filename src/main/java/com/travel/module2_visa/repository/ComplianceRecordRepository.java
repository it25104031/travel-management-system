package com.travel.module2_visa.repository;

import com.travel.module2_visa.domain.ComplianceRecordEntity;
import com.travel.module2_visa.domain.TourDepartureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComplianceRecordRepository extends JpaRepository<ComplianceRecordEntity, Long> {
    Optional<ComplianceRecordEntity> findFirstByPassportNumberAndDeparture(String passportNumber, TourDepartureEntity departure);
}
