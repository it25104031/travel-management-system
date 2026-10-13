package com.travel.module2_visa.repository;

import com.travel.module2_visa.domain.CountryEntity;
import com.travel.module2_visa.domain.VisaTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VisaTypeRepository extends JpaRepository<VisaTypeEntity, Long> {
    List<VisaTypeEntity> findByCountryAndActiveTrueOrderByNameAsc(CountryEntity country);

    List<VisaTypeEntity> findAllByOrderByCountryCountryNameAscNameAsc();

    boolean existsByCountry(CountryEntity country);
}
