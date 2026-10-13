package com.travel.module1_auth.repository;

import com.travel.module1_auth.domain.SiteCustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteCustomerRepository extends JpaRepository<SiteCustomerEntity, Long> {

    Optional<SiteCustomerEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<SiteCustomerEntity> findAllByOrderByCreatedAtDesc();
}
