package com.dahamdi.travelmanagement.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public final class DateUtils {

    private DateUtils() {
    }

    /**
     * Parses "2026-10-08T14:30" (datetime-local input) or "2026-10-08" (date input).
     * Throws IllegalArgumentException with a friendly message instead of a raw parse error.
     */
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
