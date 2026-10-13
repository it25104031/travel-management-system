package com.travel.module2_visa.repository;

import com.travel.module2_visa.domain.TourDepartureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourDepartureRepository extends JpaRepository<TourDepartureEntity, Long> {
    List<TourDepartureEntity> findAllByOrderByDepartureDateAsc();

    boolean existsByCode(String code);
}
