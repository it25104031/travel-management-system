package com.travel.module2_visa.service;

import com.travel.module2_visa.domain.VisaApplicationEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class PassportValidityService {

    public static final int REQUIRED_MONTHS = 6;

    public enum Result { VALID, EXPIRES_WITHIN_6_MONTHS, EXPIRED, INVALIDATED, UNKNOWN }

    public record PassportCheck(Result result, LocalDate expiry, LocalDate travelEnd, LocalDate requiredUntil, long daysShort) {
        public boolean isCompliant() { return result == Result.VALID; }

        public String getLabel() {
            return switch (result) {
                case VALID -> "Valid (6-month rule met)";
                case EXPIRES_WITHIN_6_MONTHS -> "Fails 6-month rule (" + daysShort + " days short)";
                case EXPIRED -> "Expired or expires before travel ends";
                case INVALIDATED -> "Reported lost: replacement required";
                case UNKNOWN -> "Not enough data to check";
            };
        }

        public String getBadgeClass() {
            return switch (result) {
                case VALID -> "badge-green";
                case UNKNOWN -> "badge-gray";
                default -> "badge-red";
            };
        }
    }

    public PassportCheck check(LocalDate expiry, LocalDate travelEnd) {
        return check(expiry, travelEnd, LocalDate.now());
    }

    public PassportCheck check(LocalDate expiry, LocalDate travelEnd, LocalDate today) {
        if (expiry == null || travelEnd == null) {
            return new PassportCheck(Result.UNKNOWN, expiry, travelEnd, null, 0);
        }
        LocalDate requiredUntil = travelEnd.plusMonths(REQUIRED_MONTHS);
        if (expiry.isBefore(today) || expiry.isBefore(travelEnd)) {
            return new PassportCheck(Result.EXPIRED, expiry, travelEnd, requiredUntil, ChronoUnit.DAYS.between(expiry, requiredUntil));
        }
        if (expiry.isBefore(requiredUntil)) {
            return new PassportCheck(Result.EXPIRES_WITHIN_6_MONTHS, expiry, travelEnd, requiredUntil,
                    ChronoUnit.DAYS.between(expiry, requiredUntil));
        }
        return new PassportCheck(Result.VALID, expiry, travelEnd, requiredUntil, 0);
    }

    public PassportCheck check(VisaApplicationEntity app) {
        if (Boolean.TRUE.equals(app.getPassportInvalidated())) {
            return new PassportCheck(Result.INVALIDATED, app.getPassportDateOfExpiry(), travelEnd(app), null, 0);
        }
        return check(app.getPassportDateOfExpiry(), travelEnd(app));
    }

    public static LocalDate travelEnd(VisaApplicationEntity app) {
        if (app.getDeparture() != null && app.getDeparture().getReturnDate() != null) {
            return app.getDeparture().getReturnDate();
        }
        return app.getExpectedDateOfArrival();
    }
}
