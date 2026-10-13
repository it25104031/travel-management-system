package com.travel.module5_packages.repository;

import com.travel.module5_packages.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItineraryRepository extends JpaRepository<Itinerary, Integer> {

    List<Itinerary> findByTourPackage_PackageIdOrderByDayNumberAsc(Integer packageId);

    boolean existsByTourPackage_PackageIdAndDayNumber(Integer packageId, Integer dayNumber);

    @Query("select max(i.dayNumber) from Itinerary i where i.tourPackage.packageId = :packageId")
    Integer findMaxDayNumberByPackageId(@Param("packageId") Integer packageId);
}
