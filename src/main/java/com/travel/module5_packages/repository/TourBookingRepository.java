package com.travel.module5_packages.repository;

import com.travel.module5_packages.entity.TourBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TourBookingRepository extends JpaRepository<TourBooking, Integer> {

    @Query("select b from TourBooking b join fetch b.tourPackage join fetch b.customer where b.customer.id = :customerId order by b.createdAt desc")
    List<TourBooking> findForCustomer(@Param("customerId") Long customerId);

    @Query("select b from TourBooking b join fetch b.tourPackage join fetch b.customer order by b.createdAt desc")
    List<TourBooking> findAllWithDetails();

    @Query("select b from TourBooking b join fetch b.tourPackage join fetch b.customer where b.invoiceId = :invoiceId")
    List<TourBooking> findByInvoiceId(@Param("invoiceId") Integer invoiceId);

    boolean existsByBookingRef(String bookingRef);

    boolean existsByTourPackage_PackageId(Integer packageId);

    long countByStatus(String status);

    @Query("select count(b) from TourBooking b where b.status = 'ACTIVE' and b.travelDate >= :from")
    long countUpcoming(@Param("from") LocalDate from);
}
