package com.travel.module1_auth.service;

import com.travel.module1_auth.domain.AuthEventType;
import com.travel.module1_auth.domain.CustomerAccountEntity;
import com.travel.module1_auth.repository.CustomerAccountRepository;
import com.travel.common.NotificationService;
import com.travel.common.SecureCodes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Issues, validates and retires the temporary reference number + passkey pairs that
 * applicants use to sign in to the customer portal.
 */
@Service
public class TempCredentialService {

    public record IssuedCredentials(String referenceNo, String passkey, LocalDateTime expiresAt, boolean emailed) { }

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final CustomerAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final AuthAuditService auditService;
    private final int validityDays;
    private final String portalUrl;

    public TempCredentialService(CustomerAccountRepository repository,
                                 PasswordEncoder passwordEncoder,
                                 NotificationService notificationService,
                                 AuthAuditService auditService,
                                 @Value("${app.customer.passkey-validity-days:30}") int validityDays,
                                 @Value("${app.customer.portal-url:http://localhost:8080/portal/login}") String portalUrl) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.validityDays = validityDays;
        this.portalUrl = portalUrl;
    }

    /** Creates the customer account for a newly submitted application and emails the credentials. */
    @Transactional
    public IssuedCredentials issue(Long applicationId, String countryCode, String email) {
        CustomerAccountEntity account = repository.findByApplicationId(applicationId).orElseGet(CustomerAccountEntity::new);
        if (account.getReferenceNo() == null) {
            account.setReferenceNo(newReferenceNo(countryCode, applicationId));
            account.setApplicationId(applicationId);
        }
        account.setEmail(email);
        return rotatePasskey(account, "issued");
    }

    /** Issues a fresh passkey for an existing reference when the applicant proves the email on file. */
    @Transactional
    public Optional<IssuedCredentials> renew(String referenceNo, String email) {
        Optional<CustomerAccountEntity> found = repository.findByReferenceNo(normalise(referenceNo));
        if (found.isEmpty() || email == null || !email.trim().equalsIgnoreCase(found.get().getEmail())) {
            return Optional.empty();
        }
        CustomerAccountEntity account = found.get();
        if (!Boolean.TRUE.equals(account.getActive()) && !account.isExpired()) {
            // Closed by staff (e.g. application rejected or travel completed); renewal not allowed
            return Optional.empty();
        }
        return Optional.of(rotatePasskey(account, "renewed"));
    }

    private IssuedCredentials rotatePasskey(CustomerAccountEntity account, String verb) {
        String passkey = SecureCodes.random(10);
        account.setPasskeyHash(passwordEncoder.encode(passkey));
        account.setExpiresAt(LocalDateTime.now().plusDays(validityDays));
        account.setActive(true);
        repository.save(account);

        boolean emailed = notificationService.send(account.getEmail(),
                "Your visa application portal access (" + account.getReferenceNo() + ")",
                "Your application portal credentials have been " + verb + ".\n\n"
                        + "Reference number: " + account.getReferenceNo() + "\n"
                        + "Passkey: " + passkey + "\n"
                        + "Valid until: " + account.getExpiresAt().format(DISPLAY) + "\n\n"
                        + "Sign in at " + portalUrl + " to check your status, upload requested documents "
                        + "and complete your pre-departure checklist.");
        auditService.record(account.getReferenceNo(), AuthEventType.CUSTOMER_CREDENTIALS_ISSUED, null,
                "Credentials " + verb + " for application " + account.getApplicationId());
        return new IssuedCredentials(account.getReferenceNo(), passkey, account.getExpiresAt(), emailed);
    }

    @Transactional(readOnly = true)
    public Optional<CustomerAccountEntity> findByReference(String referenceNo) {
        return repository.findByReferenceNo(normalise(referenceNo));
    }

    @Transactional(readOnly = true)
    public Optional<CustomerAccountEntity> findByApplication(Long applicationId) {
        return repository.findByApplicationId(applicationId);
    }

    @Transactional
    public void markLoggedIn(String referenceNo) {
        repository.findByReferenceNo(referenceNo).ifPresent(a -> a.setLastLoginAt(LocalDateTime.now()));
    }

    /** Ends portal access when the application is closed. Active sessions are cut on their next request. */
    @Transactional
    public void deactivateForApplication(Long applicationId, String reason) {
        repository.findByApplicationId(applicationId).ifPresent(account -> {
            if (Boolean.TRUE.equals(account.getActive())) {
                account.setActive(false);
                auditService.record(account.getReferenceNo(), AuthEventType.CUSTOMER_SESSION_TERMINATED, null, reason);
            }
        });
    }

    /** Called by the daily scheduler: flags expired accounts as inactive. Returns how many were closed. */
    @Transactional
    public int deactivateExpired() {
        List<CustomerAccountEntity> expired = repository.findByActiveTrueAndExpiresAtBefore(LocalDateTime.now());
        expired.forEach(a -> {
            a.setActive(false);
            auditService.record(a.getReferenceNo(), AuthEventType.CUSTOMER_SESSION_TERMINATED, null, "Passkey expired");
        });
        return expired.size();
    }

    private String newReferenceNo(String countryCode, Long applicationId) {
        String prefix = (countryCode == null || countryCode.isBlank() ? "VA" : countryCode.toUpperCase());
        String candidate = String.format("%s-%d-%06d", prefix, Year.now().getValue(), applicationId);
        while (repository.existsByReferenceNo(candidate)) {
            candidate = candidate + SecureCodes.random(1).toUpperCase();
        }
        return candidate;
    }

    public static String normalise(String referenceNo) {
        return referenceNo == null ? "" : referenceNo.trim().toUpperCase();
    }
}
