package com.travel.module2_visa.repository;

import com.travel.module2_visa.domain.VisaApplicationEntity;
import com.travel.module2_visa.domain.TourDepartureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisaApplicationRepository extends JpaRepository<VisaApplicationEntity, Long> {
    List<VisaApplicationEntity> findByDeparture(TourDepartureEntity departure);

    List<VisaApplicationEntity> findByDepartureIsNullOrderBySurnameAsc();

    List<VisaApplicationEntity> findByPassportNumber(String passportNumber);

    // A traveller's applications: the ones they submitted while signed in, plus older ones sent with their email
    List<VisaApplicationEntity> findBySiteCustomerIdOrEmailIdIgnoreCaseOrderByCreatedAtDesc(Long siteCustomerId, String emailId);
}
