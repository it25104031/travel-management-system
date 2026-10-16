package com.travel.module3_billing;

import com.travel.module3_billing.service.PaymentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        BigDecimal paid = new BigDecimal("0.10").add(new BigDecimal("0.20"));
        assertEquals("PAID", PaymentService.calculateStatus(new BigDecimal("0.30"), paid));
    }
}
