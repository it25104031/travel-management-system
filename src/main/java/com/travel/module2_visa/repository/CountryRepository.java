package com.travel.module2_visa.repository;

import com.travel.module2_visa.domain.CountryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CountryRepository extends JpaRepository<CountryEntity, Long> {
    List<CountryEntity> findByIsActiveTrue();

    Optional<CountryEntity> findByCountryCode(String countryCode);

    Optional<CountryEntity> findByCountryNameIgnoreCase(String countryName);
}
