package com.travel.module4_logistics.repository;

import com.travel.module4_logistics.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    List<Supplier> findAllByOrderByNameAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Supplier s where s.id = :id")
    Optional<Supplier> findForAllocation(@Param("id") Integer id);
}
