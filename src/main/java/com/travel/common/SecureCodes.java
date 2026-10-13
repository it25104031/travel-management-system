package com.travel.common;

import java.security.SecureRandom;

/** Cryptographically random codes for passkeys and temporary passwords. */
public final class SecureCodes {

    // No 0/O/1/I/l to avoid confusion when customers type them in
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureCodes() { }

    public static String random(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
