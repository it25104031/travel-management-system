package com.travel.module2_visa.repository;

import com.travel.module2_visa.domain.RequiredDocumentEntity;
import com.travel.module2_visa.domain.VisaTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequiredDocumentRepository extends JpaRepository<RequiredDocumentEntity, Long> {
    List<RequiredDocumentEntity> findByVisaTypeOrderByIdAsc(VisaTypeEntity visaType);
}
