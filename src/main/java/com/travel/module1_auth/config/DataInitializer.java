package com.travel.module1_auth.config;

import com.travel.module1_auth.domain.AdminUserEntity;
import com.travel.module1_auth.repository.AdminUserRepository;
import com.travel.module1_auth.service.AdminUserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(AdminUserRepository repository, PasswordEncoder passwordEncoder) {
        return args -> {

            if (repository.findByUsername("admin") == null) {
                AdminUserEntity admin = new AdminUserEntity();
                admin.setUsername("admin");
                admin.setFullName("System Administrator");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole("ADMIN");
                repository.save(admin);
            }

            if (repository.findByUsername("operator") == null) {
                AdminUserEntity operator = new AdminUserEntity();
                operator.setUsername("operator");
                operator.setFullName("Office Operator");
                operator.setPassword(passwordEncoder.encode("operator123"));
                operator.setRole("USER");
                repository.save(operator);
            }

            for (AdminUserEntity user : repository.findAll()) {
                boolean changed = false;
                String role = AdminUserService.storedRole(user.getRole());
                if (!role.equals(user.getRole())) { user.setRole(role); changed = true; }
                if (user.getActive() == null) { user.setActive(true); changed = true; }
                if (user.getMustChangePassword() == null) { user.setMustChangePassword(false); changed = true; }
                if (changed) repository.save(user);
            }
        };
    }
}
