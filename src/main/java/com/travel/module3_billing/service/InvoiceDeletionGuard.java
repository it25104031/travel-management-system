package com.travel.module3_billing.service;

public interface InvoiceDeletionGuard {

    String blockReason(Integer invoiceId);
}
