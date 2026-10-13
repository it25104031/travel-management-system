package com.travel.module3_billing.service;

/**
 * Lets other modules stop an invoice from being deleted while they still depend on it
 * (e.g. Module 5 tour bookings paid through the invoice). Billing does not need to know
 * about those modules - it simply asks every guard before deleting.
 */
public interface InvoiceDeletionGuard {

    /** @return a message explaining why the invoice can't be deleted, or null if it can. */
    String blockReason(Integer invoiceId);
}
