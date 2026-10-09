package com.dahamdi.travelmanagement;

import com.dahamdi.travelmanagement.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    public DataInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {

        // Default admin (password is stored as a BCrypt hash)
        if (userService.findByUsername("admin").isEmpty()) {
            userService.createUserWithRole("admin", "admin123",
                    "System Administrator", "admin@dahamdi.com", "ADMIN");
            System.out.println(">>> Default ADMIN account created (username: admin) - see README for the demo password");
        }

        // Default user
        if (userService.findByUsername("user1").isEmpty()) {
            userService.createUserWithRole("user1", "user123",
                    "Amal Perera", "amal@gmail.com", "USER");
            System.out.println(">>> Default USER account created (username: user1) - see README for the demo password");
        }
    }
}
