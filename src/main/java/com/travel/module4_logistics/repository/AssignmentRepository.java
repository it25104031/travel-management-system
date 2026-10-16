package com.travel.module4_logistics.repository;

import com.travel.module4_logistics.entity.Assignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {
    boolean existsByGuide_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Integer guideId, LocalDate end, LocalDate start);
    List<Assignment> findByDeparture_Id(Integer departureId);

    @EntityGraph(attributePaths = {"guide","departure"})
    List<Assignment> findAllByOrderByStartDateDesc();
}
