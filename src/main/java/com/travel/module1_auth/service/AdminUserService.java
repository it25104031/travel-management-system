package com.travel.module1_auth.service;

import com.travel.module1_auth.domain.AdminUserEntity;
import com.travel.module1_auth.domain.AuthEventType;
import com.travel.module1_auth.repository.AdminUserRepository;
import com.travel.common.SecureCodes;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService implements UserDetailsService {

    public static final int MIN_PASSWORD_LENGTH = 8;

    private final AdminUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditService auditService;

    public AdminUserService(AdminUserRepository repository, PasswordEncoder passwordEncoder, AuthAuditService auditService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUserEntity userEntity = repository.findByUsername(username);

        if (userEntity == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return User.builder()
                .username(userEntity.getUsername())
                .password(userEntity.getPassword())
                .authorities(normaliseRole(userEntity.getRole()))
                .disabled(!userEntity.isActiveAccount())
                .accountLocked(auditService.isLockedOut(username, AuthEventType.LOGIN_FAILURE, AuthEventType.LOGIN_SUCCESS))
                .build();
    }

    public static String normaliseRole(String role) {
        if (role == null || role.isBlank()) return "ROLE_USER";
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }

    public static String storedRole(String role) {
        return normaliseRole(role).substring("ROLE_".length());
    }

    @Transactional(readOnly = true)
    public List<AdminUserEntity> findAll() {
        return repository.findAllByOrderByUsernameAsc();
    }

    @Transactional(readOnly = true)
    public AdminUserEntity get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown user id " + id));
    }

    @Transactional(readOnly = true)
    public AdminUserEntity findByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Transactional
    public AdminUserEntity createUser(String username, String fullName, String email, String rawPassword, String role,
                                      String actor, String ip) {
        username = username == null ? "" : username.trim();
        if (username.isEmpty()) throw new IllegalArgumentException("Username is required.");
        if (repository.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username '" + username + "' already exists.");
        }
        validatePassword(rawPassword);
        AdminUserEntity user = new AdminUserEntity();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(storedRole(role));
        user.setActive(true);
        repository.save(user);
        auditService.record(actor, AuthEventType.USER_CREATED, ip, "Created " + username + " as " + user.getRole());
        return user;
    }

    @Transactional
    public void updateProfile(Long id, String fullName, String email, String role, String actor, String ip) {
        AdminUserEntity user = get(id);
        user.setFullName(fullName);
        user.setEmail(email);
        String newRole = storedRole(role);
        if (!newRole.equals(storedRole(user.getRole()))) {
            if (user.getUsername().equals(actor)) {
                throw new IllegalArgumentException("You cannot change your own role.");
            }
            auditService.record(actor, AuthEventType.ROLE_CHANGED, ip,
                    user.getUsername() + ": " + user.getRole() + " -> " + newRole);
            user.setRole(newRole);
        }
    }

    @Transactional
    public void setActive(Long id, boolean active, String actor, String ip) {
        AdminUserEntity user = get(id);
        if (!active && user.getUsername().equals(actor)) {
            throw new IllegalArgumentException("You cannot deactivate your own account.");
        }
        user.setActive(active);
        auditService.record(actor, active ? AuthEventType.USER_REACTIVATED : AuthEventType.USER_DEACTIVATED, ip, user.getUsername());
    }

    @Transactional
    public String resetPassword(Long id, String actor, String ip) {
        AdminUserEntity user = get(id);
        String temporary = SecureCodes.random(10);
        user.setPassword(passwordEncoder.encode(temporary));
        user.setMustChangePassword(true);
        auditService.record(actor, AuthEventType.PASSWORD_RESET, ip, "Forced reset for " + user.getUsername());
        return temporary;
    }

    @Transactional
    public void changeOwnPassword(String username, String currentPassword, String newPassword, String confirmPassword, String ip) {
        AdminUserEntity user = repository.findByUsername(username);
        if (user == null) throw new IllegalArgumentException("Unknown user.");
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Your current password is incorrect.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("The new passwords do not match.");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("The new password must be different from the current one.");
        }
        validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        auditService.record(username, AuthEventType.PASSWORD_CHANGE, ip, null);
    }

    private void validatePassword(String raw) {
        if (raw == null || raw.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Passwords must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
    }
}
