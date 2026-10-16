package com.travel.module2_visa.service;

public final class MaskingUtil {

    private MaskingUtil() { }

    public static String mask(String value) {
        if (value == null || value.isBlank()) return value;
        String v = value.trim();
        if (v.length() <= 4) return "*".repeat(v.length());
        return v.substring(0, 2) + "*".repeat(v.length() - 4) + v.substring(v.length() - 2);
    }

    public static boolean isMasked(String value) {
        return value != null && value.contains("*");
    }
}
