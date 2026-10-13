package com.travel.module1_auth.service;

import com.travel.module1_auth.domain.AuthEventType;
import com.travel.module1_auth.domain.SiteCustomerEntity;
import com.travel.module1_auth.repository.SiteCustomerRepository;
import com.travel.common.ValidationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Website traveller accounts. Travellers sign in on the same /login page as staff, with their
 * email address instead of a username, and receive ROLE_MEMBER.
 */
@Service
public class SiteCustomerService implements UserDetailsService {

    public static final String ROLE_MEMBER = "ROLE_MEMBER";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^[+0-9 ()-]{7,20}$");

    private final SiteCustomerRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthAuditService auditService;

    public SiteCustomerService(SiteCustomerRepository repository, PasswordEncoder passwordEncoder,
                               AuthAuditService auditService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String key = normaliseEmail(email);
        SiteCustomerEntity customer = repository.findByEmailIgnoreCase(key)
                .orElseThrow(() -> new UsernameNotFoundException("No traveller account"));
        return User.builder()
                .username(customer.getEmail())
                .password(customer.getPasswordHash())
                .authorities(ROLE_MEMBER)
                .disabled(!customer.isActiveAccount())
                .accountLocked(auditService.isLockedOut(customer.getEmail(), AuthEventType.LOGIN_FAILURE, AuthEventType.LOGIN_SUCCESS))
                .build();
    }

    @Transactional
    public SiteCustomerEntity register(String fullName, String email, String phone, String password,
                                       String confirmPassword, String ip) {
        List<String> errors = new ArrayList<>();
        String name = fullName == null ? "" : fullName.trim();
        String mail = normaliseEmail(email);
        String tel = phone == null ? "" : phone.trim();

        if (name.length() < 2) errors.add("Please enter your full name.");
        if (name.length() > 120) errors.add("Full name must be 120 characters or fewer.");
        if (!EMAIL.matcher(mail).matches()) errors.add("Please enter a valid email address.");
        else if (repository.existsByEmailIgnoreCase(mail)) errors.add("An account with this email already exists. Please sign in instead.");
        if (!tel.isEmpty() && !PHONE.matcher(tel).matches()) errors.add("Please enter a valid phone number.");
        checkPassword(password, confirmPassword, errors);
        if (!errors.isEmpty()) throw new ValidationException(errors);

        SiteCustomerEntity customer = new SiteCustomerEntity();
        customer.setFullName(name);
        customer.setEmail(mail);
        customer.setPhone(tel.isEmpty() ? null : tel);
        customer.setPasswordHash(passwordEncoder.encode(password));
        customer.setActive(true);
        customer.setLastLoginAt(LocalDateTime.now());
        repository.save(customer);
        auditService.record(mail, AuthEventType.MEMBER_REGISTERED, ip, "Website sign-up");
        return customer;
    }

    @Transactional
    public void updateProfile(Long id, String fullName, String phone, String ip) {
        SiteCustomerEntity customer = get(id);
        List<String> errors = new ArrayList<>();
        String name = fullName == null ? "" : fullName.trim();
        String tel = phone == null ? "" : phone.trim();
        if (name.length() < 2) errors.add("Please enter your full name.");
        if (name.length() > 120) errors.add("Full name must be 120 characters or fewer.");
        if (!tel.isEmpty() && !PHONE.matcher(tel).matches()) errors.add("Please enter a valid phone number.");
        if (!errors.isEmpty()) throw new ValidationException(errors);
        customer.setFullName(name);
        customer.setPhone(tel.isEmpty() ? null : tel);
        auditService.record(customer.getEmail(), AuthEventType.MEMBER_PROFILE_UPDATED, ip, null);
    }

    @Transactional
    public void changePassword(Long id, String current, String password, String confirm, String ip) {
        SiteCustomerEntity customer = get(id);
        List<String> errors = new ArrayList<>();
        if (current == null || !passwordEncoder.matches(current, customer.getPasswordHash())) {
            errors.add("Your current password is incorrect.");
        }
        checkPassword(password, confirm, errors);
        if (errors.isEmpty() && passwordEncoder.matches(password, customer.getPasswordHash())) {
            errors.add("The new password must be different from the current one.");
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
        customer.setPasswordHash(passwordEncoder.encode(password));
        auditService.record(customer.getEmail(), AuthEventType.PASSWORD_CHANGE, ip, "Website account");
    }

    @Transactional
    public void markLoggedIn(String email) {
        repository.findByEmailIgnoreCase(normaliseEmail(email)).ifPresent(c -> c.setLastLoginAt(LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public SiteCustomerEntity get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown traveller account."));
    }

    @Transactional(readOnly = true)
    public Optional<SiteCustomerEntity> findByEmail(String email) {
        return repository.findByEmailIgnoreCase(normaliseEmail(email));
    }

    @Transactional(readOnly = true)
    public List<SiteCustomerEntity> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void setActive(Long id, boolean active, String actor, String ip) {
        SiteCustomerEntity customer = get(id);
        customer.setActive(active);
        auditService.record(actor, active ? AuthEventType.USER_REACTIVATED : AuthEventType.USER_DEACTIVATED, ip,
                "Traveller " + customer.getEmail());
    }

    /** The signed-in traveller, if the current user is one. */
    @Transactional(readOnly = true)
    public Optional<SiteCustomerEntity> current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !isMember(auth)) return Optional.empty();
        return repository.findByEmailIgnoreCase(auth.getName());
    }

    /** The signed-in traveller; throws if the current user is not one. */
    @Transactional(readOnly = true)
    public SiteCustomerEntity requireCurrent() {
        return current().orElseThrow(() -> new IllegalStateException("Please sign in with your traveller account."));
    }

    public static boolean isMember(Authentication auth) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> ROLE_MEMBER.equals(a.getAuthority()));
    }

    public static String normaliseEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static void checkPassword(String password, String confirm, List<String> errors) {
        if (password == null || password.length() < AdminUserService.MIN_PASSWORD_LENGTH) {
            errors.add("Passwords must be at least " + AdminUserService.MIN_PASSWORD_LENGTH + " characters.");
        } else if (!password.chars().anyMatch(Character::isDigit) || !password.chars().anyMatch(Character::isLetter)) {
            errors.add("Passwords must contain at least one letter and one number.");
        }
        if (password != null && !password.equals(confirm)) errors.add("The two passwords do not match.");
    }
}
