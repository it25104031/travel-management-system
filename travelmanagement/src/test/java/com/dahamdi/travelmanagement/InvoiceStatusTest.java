package com.dahamdi.travelmanagement;

import com.dahamdi.travelmanagement.service.PaymentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Plain unit test - runs without a database. */
class InvoiceStatusTest {

    private static String status(String total, String paid) {
        return PaymentService.calculateStatus(new BigDecimal(total), new BigDecimal(paid));
    }

    @Test
    void nothingPaidIsUnpaid() {
        assertEquals("UNPAID", status("250000.00", "0"));
    }

    @Test
    void somePaidIsPartial() {
        assertEquals("PARTIAL", status("320000.00", "100000.00"));
    }

    @Test
    void fullyPaidIsPaid() {
        assertEquals("PAID", status("180000.00", "180000.00"));
    }

    @Test
    void decimalsAreExact() {
        // With double, 0.1 + 0.2 = 0.30000000000000004 and this would wrongly be PAID/PARTIAL
        BigDecimal paid = new BigDecimal("0.10").add(new BigDecimal("0.20"));
        assertEquals("PAID", PaymentService.calculateStatus(new BigDecimal("0.30"), paid));
    }
}
