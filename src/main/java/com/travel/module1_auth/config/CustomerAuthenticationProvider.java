package com.travel.module1_auth.config;

import com.travel.module1_auth.domain.AuthEventType;
import com.travel.module1_auth.domain.CustomerAccountEntity;
import com.travel.module1_auth.service.AuthAuditService;
import com.travel.module1_auth.service.TempCredentialService;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/** Validates a reference number + temporary passkey and grants ROLE_CUSTOMER. Not a bean on purpose. */
public class CustomerAuthenticationProvider implements AuthenticationProvider {

    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";

    private final TempCredentialService credentialService;
    private final AuthAuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public CustomerAuthenticationProvider(TempCredentialService credentialService,
                                          AuthAuditService auditService,
                                          PasswordEncoder passwordEncoder) {
        this.credentialService = credentialService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String referenceNo = TempCredentialService.normalise(authentication.getName());
        String passkey = authentication.getCredentials() == null ? "" : authentication.getCredentials().toString().trim();

        if (auditService.isLockedOut(referenceNo, AuthEventType.CUSTOMER_LOGIN_FAILURE, AuthEventType.CUSTOMER_LOGIN_SUCCESS)) {
            throw new LockedException("Too many failed attempts");
        }

        CustomerAccountEntity account = credentialService.findByReference(referenceNo)
                .orElseThrow(() -> new BadCredentialsException("Invalid reference number or passkey"));

        if (!passwordEncoder.matches(passkey, account.getPasskeyHash())) {
            throw new BadCredentialsException("Invalid reference number or passkey");
        }
        if (account.isExpired()) {
            throw new CredentialsExpiredException("Passkey expired");
        }
        if (!Boolean.TRUE.equals(account.getActive()) || account.getApplicationId() == null) {
            throw new DisabledException("Portal access closed");
        }

        return UsernamePasswordAuthenticationToken.authenticated(
                account.getReferenceNo(), null, List.of(new SimpleGrantedAuthority(ROLE_CUSTOMER)));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
