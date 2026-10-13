package com.travel.module4_logistics.repository;
import com.travel.module4_logistics.entity.Departure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DepartureRepository extends JpaRepository<Departure, Integer> {
    boolean existsByReferenceIgnoreCase(String reference);
    List<Departure> findAllByOrderByStartDateAsc();
}
