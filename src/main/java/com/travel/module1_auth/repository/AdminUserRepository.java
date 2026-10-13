package com.travel.module1_auth.repository;

import com.travel.module1_auth.domain.AdminUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUserEntity, Long> {
    AdminUserEntity findByUsername(String username);

    List<AdminUserEntity> findAllByOrderByUsernameAsc();
}
