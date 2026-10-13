package com.travel.module1_auth.repository;

import com.travel.module1_auth.domain.CustomerAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccountEntity, Long> {
    Optional<CustomerAccountEntity> findByReferenceNo(String referenceNo);

    Optional<CustomerAccountEntity> findByApplicationId(Long applicationId);

    boolean existsByReferenceNo(String referenceNo);

    List<CustomerAccountEntity> findByActiveTrueAndExpiresAtBefore(LocalDateTime time);
}
