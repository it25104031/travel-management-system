package com.travel.module3_billing.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public final class DateUtils {

    private DateUtils() {
    }

    public static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Date is required.");
        }
        try {
            String trimmed = value.trim();
            if (trimmed.contains("T")) {
                return LocalDateTime.parse(trimmed);
            }
            return LocalDate.parse(trimmed).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + value);
        }
    }
}
