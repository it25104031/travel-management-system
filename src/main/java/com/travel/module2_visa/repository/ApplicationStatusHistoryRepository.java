package com.travel.module2_visa.repository;

import com.travel.module2_visa.domain.ApplicationStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistoryEntity, Long> {
    List<ApplicationStatusHistoryEntity> findByApplicationTypeAndApplicationIdOrderByChangedAtAsc(String applicationType, Long applicationId);

    List<ApplicationStatusHistoryEntity> findByToStatusIn(List<String> statuses);
}
