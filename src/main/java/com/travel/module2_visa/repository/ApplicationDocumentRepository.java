package com.travel.module2_visa.repository;

import com.travel.module2_visa.domain.ApplicationDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocumentEntity, Long> {
    List<ApplicationDocumentEntity> findByStatus(String status);
}
