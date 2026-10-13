package com.travel;

import com.travel.module2_visa.service.PassportValidityService;
import com.travel.module2_visa.service.PassportValidityService.Result;
import com.travel.module2_visa.service.ReadinessService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PassportValidityServiceTest {

    private final PassportValidityService service = new PassportValidityService();
    private final LocalDate today = LocalDate.of(2026, 10, 1);
    private final LocalDate returnDate = LocalDate.of(2026, 12, 15);

    @Test
    void validWhenPassportOutlastsReturnBySixMonths() {
        assertEquals(Result.VALID, service.check(LocalDate.of(2027, 6, 15), returnDate, today).result());
    }

    @Test
    void exactlySixMonthsAfterReturnIsValid() {
        assertEquals(Result.VALID, service.check(returnDate.plusMonths(6), returnDate, today).result());
    }

    @Test
    void oneDayShortOfSixMonthsFails() {
        var check = service.check(returnDate.plusMonths(6).minusDays(1), returnDate, today);
        assertEquals(Result.EXPIRES_WITHIN_6_MONTHS, check.result());
        assertEquals(1, check.daysShort());
    }

    @Test
    void expiringBeforeReturnIsExpired() {
        assertEquals(Result.EXPIRED, service.check(LocalDate.of(2026, 12, 1), returnDate, today).result());
    }

    @Test
    void missingDatesAreUnknown() {
        assertEquals(Result.UNKNOWN, service.check(null, returnDate, today).result());
    }

    @Test
    void readinessPercentNeverRoundsUpToHundred() {
        assertEquals(new BigDecimal("99.99"), ReadinessService.percent(9999, 10000));
        assertEquals(new BigDecimal("100.00"), ReadinessService.percent(5, 5));
        assertEquals(new BigDecimal("66.66"), ReadinessService.percent(2, 3));
        assertEquals(new BigDecimal("0.00"), ReadinessService.percent(0, 0));
    }
}
