package com.dahamdi.travelmanagement.service;

import com.dahamdi.travelmanagement.entity.User;
import com.dahamdi.travelmanagement.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Passwords are stored as BCrypt hashes, never as plain text.
 * Old accounts that still have a plain-text password (e.g. from Database.sql)
 * are upgraded to a hash automatically the first time they log in.
 */
@Service
public class UserService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public Optional<User> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        String stored = user.getPassword();

        if (isBcryptHash(stored)) {
            return passwordEncoder.matches(password, stored) ? Optional.of(user) : Optional.empty();
        }

        // Legacy plain-text password: check it once, then replace it with a hash.
        if (stored != null && stored.equals(password)) {
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    @Transactional
    public User register(String username, String password, String fullName, String email) {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanFullName = fullName == null ? "" : fullName.trim();
        String cleanEmail = email == null ? "" : email.trim();

        if (!USERNAME.matcher(cleanUsername).matches()) {
            throw new IllegalArgumentException("Username must be 3-50 characters (letters, numbers, . _ -).");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        if (cleanFullName.isEmpty() || cleanFullName.length() > 100) {
            throw new IllegalArgumentException("Full name is required (max 100 characters).");
        }
        if (!EMAIL.matcher(cleanEmail).matches() || cleanEmail.length() > 100) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (userRepository.findByUsername(cleanUsername).isPresent()) {
            throw new IllegalArgumentException("Username already exists. Please choose a different one.");
        }

        User user = new User();
        user.setUsername(cleanUsername);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(cleanFullName);
        user.setEmail(cleanEmail);
        user.setRole("USER");

        return userRepository.save(user);
    }

    /** Used by DataInitializer to create the default accounts with hashed passwords. */
    @Transactional
    public User createUserWithRole(String username, String password, String fullName, String email, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public long countUsers() {
        return userRepository.count();
    }

    public Optional<User> getUserById(Integer id) {
        return userRepository.findById(id);
    }

    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }

    /**
     * A copy of the user WITHOUT the password, safe to keep in the HTTP session.
     */
    public static User toSessionUser(User user) {
        User copy = new User();
        copy.setUserId(user.getUserId());
        copy.setUsername(user.getUsername());
        copy.setFullName(user.getFullName());
        copy.setEmail(user.getEmail());
        copy.setRole(user.getRole());
        return copy;
    }

    private static boolean isBcryptHash(String value) {
        return value != null && value.length() == 60
                && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
