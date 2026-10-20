package com.travel.module4_logistics.repository;

import com.travel.module4_logistics.entity.HotelBooking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface HotelBookingRepository extends JpaRepository<HotelBooking, Integer> {
    List<HotelBooking> findBySupplier_IdAndCheckInLessThanAndCheckOutGreaterThan(Integer supplierId, LocalDate end, LocalDate start);
    List<HotelBooking> findByDeparture_Id(Integer departureId);

    @EntityGraph(attributePaths = {"supplier","departure"})
    List<HotelBooking> findAllByOrderByCheckInDesc();
}
