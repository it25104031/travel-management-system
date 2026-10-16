package com.travel.module3_billing.config;

import com.travel.module1_auth.domain.AdminUserEntity;
import com.travel.module1_auth.repository.AdminUserRepository;
import com.travel.module3_billing.exception.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentStaff {

    private final AdminUserRepository adminUserRepository;

    public CurrentStaff(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    public AdminUserEntity get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return adminUserRepository.findByUsername(auth.getName());
    }

    public AdminUserEntity require() {
        AdminUserEntity user = get();
        if (user == null) {
            throw new NotFoundException("Your account no longer exists. Please log in again.");
        }
        return user;
    }

    public BillingUserView view() {
        AdminUserEntity user = get();
        String name = user == null ? "Staff"
                : (user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getUsername());
        return new BillingUserView(user == null ? null : user.getId(), user == null ? null : user.getUsername(), name, "ADMIN");
    }

    public record BillingUserView(Long userId, String username, String fullName, String role) {
    }
}
