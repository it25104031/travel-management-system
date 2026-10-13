package com.travel.module4_logistics.repository;

import com.travel.module4_logistics.entity.Guide;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GuideRepository extends JpaRepository<Guide, Integer> {
    // Load the linked records with the list (open-in-view is off in the combined app)
    @EntityGraph(attributePaths = {"supplier"})
    List<Guide> findAllByOrderByNameAsc();
}
