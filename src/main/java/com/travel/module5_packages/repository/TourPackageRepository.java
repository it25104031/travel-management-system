package com.travel.module5_packages.repository;

import com.travel.module5_packages.entity.TourPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TourPackageRepository extends JpaRepository<TourPackage, Integer> {

    @Query("select distinct p from TourPackage p left join fetch p.itineraries order by p.packageId")
    List<TourPackage> findAllWithItineraries();
}
