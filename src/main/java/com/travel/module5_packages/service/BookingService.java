package com.travel.module5_packages.service;

import com.travel.common.ValidationException;
import com.travel.module1_auth.domain.SiteCustomerEntity;
import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.service.InvoiceDeletionGuard;
import com.travel.module3_billing.repository.PaymentRepository;
import com.travel.module3_billing.repository.PaymentSlipRepository;
import com.travel.module3_billing.service.InvoiceService;
import com.travel.module3_billing.service.PaymentService;
import com.travel.module5_packages.entity.TourBooking;
import com.travel.module5_packages.entity.TourPackage;
import com.travel.module5_packages.repository.TourBookingRepository;
import com.travel.module5_packages.repository.TourPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Turns a traveller's cart into bookings. One checkout = one invoice in Billing (Module 3)
 * for the whole cart + one booking per item, all in a single transaction.
 */
@Service
public class BookingService implements InvoiceDeletionGuard {

    public static final int MAX_TRAVELLERS = 20;
    public static final int MIN_DAYS_AHEAD = 3;

    private final TourBookingRepository bookingRepository;
    private final TourPackageRepository packageRepository;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final PaymentSlipRepository slipRepository;

    public BookingService(TourBookingRepository bookingRepository, TourPackageRepository packageRepository,
                          InvoiceService invoiceService, PaymentService paymentService,
                          PaymentRepository paymentRepository, PaymentSlipRepository slipRepository) {
        this.bookingRepository = bookingRepository;
        this.packageRepository = packageRepository;
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.slipRepository = slipRepository;
    }

    /**
     * Lets a traveller remove a trip they haven't paid anything for.
     * The trip's share comes off the invoice; if it was the only trip on the invoice, the invoice goes too.
     * If the invoice has any payment history (e.g. a rejected slip) it is kept and the trip is cancelled instead.
     *
     * @return true when the trip was deleted, false when it was cancelled instead
     */
    @Transactional
    public boolean deleteUnpaid(SiteCustomerEntity customer, Integer bookingId) {
        TourBooking booking = bookingId == null ? null : bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || customer == null || booking.getCustomer() == null
                || !booking.getCustomer().getId().equals(customer.getId())) {
            throw new ValidationException("That trip couldn't be found.");
        }
        if (booking.isCancelled()) throw new ValidationException("This trip is already cancelled.");

        Integer invoiceId = booking.getInvoiceId();
        Invoice invoice = invoiceId == null ? null : invoiceService.getInvoiceById(invoiceId).orElse(null);
        if (invoice != null) {
            if (paymentService.getCompletedPaymentTotal(invoiceId).signum() > 0) {
                throw new ValidationException("Paid or part-paid trips can't be deleted. Contact us if you need to cancel.");
            }
            if (slipRepository.existsByInvoice_InvoiceIdAndStatus(invoiceId, com.travel.module3_billing.service.PaymentSlipService.PENDING)) {
                throw new ValidationException("Your bank slip for this trip is being checked, so it can't be deleted right now.");
            }
        }

        List<TourBooking> others = invoiceId == null ? List.of() : bookingRepository.findByInvoiceId(invoiceId).stream()
                .filter(b -> !b.getBookingId().equals(booking.getBookingId()) && !b.isCancelled()).toList();
        boolean invoiceHasHistory = invoice != null && (paymentRepository.existsByInvoice_InvoiceId(invoiceId)
                || slipRepository.existsByInvoice_InvoiceId(invoiceId));

        if (invoice != null && !others.isEmpty()) {
            // Other trips still on this invoice: just take this trip's amount off the bill.
            BigDecimal newTotal = invoice.getTotalAmount().subtract(booking.getTotalAmount());
            if (newTotal.signum() <= 0) newTotal = others.stream().map(TourBooking::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            invoiceService.updateInvoice(invoiceId, invoice.getInvoiceNumber(), invoice.getCustomerName(),
                    invoice.getInvoiceDate(), newTotal);
        } else if (invoiceHasHistory) {
            booking.setStatus(TourBooking.CANCELLED);
            return false;
        }

        bookingRepository.delete(booking);
        bookingRepository.flush();
        if (invoice != null && others.isEmpty()) {
            invoiceService.deleteInvoice(invoiceId);
        }
        return true;
    }

    public record BookingRequest(Integer packageId, LocalDate travelDate, int travellers) { }

    public record CheckoutResult(Invoice invoice, List<TourBooking> bookings) { }

    /** Checks one cart line; returns the problems (empty when it can be booked). */
    @Transactional(readOnly = true)
    public List<String> check(BookingRequest request) {
        List<String> errors = new ArrayList<>();
        TourPackage pkg = request.packageId() == null ? null : packageRepository.findById(request.packageId()).orElse(null);
        if (pkg == null) {
            errors.add("A package in your cart no longer exists.");
            return errors;
        }
        if (!pkg.isAvailable()) errors.add(pkg.getPackageName() + " is not available for booking right now.");
        if (request.travelDate() == null) {
            errors.add("Choose a travel date for " + pkg.getPackageName() + ".");
        } else if (request.travelDate().isBefore(earliestTravelDate())) {
            errors.add("The travel date for " + pkg.getPackageName() + " must be on or after " + earliestTravelDate() + ".");
        } else if (request.travelDate().isAfter(LocalDate.now().plusYears(2))) {
            errors.add("The travel date for " + pkg.getPackageName() + " must be within the next two years.");
        }
        if (request.travellers() < 1 || request.travellers() > MAX_TRAVELLERS) {
            errors.add("Travellers for " + pkg.getPackageName() + " must be between 1 and " + MAX_TRAVELLERS + ".");
        }
        return errors;
    }

    public static LocalDate earliestTravelDate() {
        return LocalDate.now().plusDays(MIN_DAYS_AHEAD);
    }

    @Transactional
    public CheckoutResult checkout(SiteCustomerEntity customer, List<BookingRequest> requests, String notes) {
        if (customer == null) throw new ValidationException("Please sign in to complete your booking.");
        if (requests == null || requests.isEmpty()) throw new ValidationException("Your cart is empty.");

        List<String> errors = new ArrayList<>();
        for (BookingRequest r : requests) errors.addAll(check(r));
        if (notes != null && notes.length() > 500) errors.add("Notes must be 500 characters or fewer.");
        if (!errors.isEmpty()) throw new ValidationException(errors);

        BigDecimal total = BigDecimal.ZERO;
        List<TourBooking> bookings = new ArrayList<>();
        for (BookingRequest r : requests) {
            TourPackage pkg = packageRepository.findById(r.packageId()).orElseThrow();
            BigDecimal line = pkg.getPrice().multiply(BigDecimal.valueOf(r.travellers()));
            total = total.add(line);

            TourBooking b = new TourBooking();
            b.setBookingRef(newReference());
            b.setCustomer(customer);
            b.setTourPackage(pkg);
            b.setTravelDate(r.travelDate());
            b.setTravellers(r.travellers());
            b.setUnitPrice(pkg.getPrice());
            b.setTotalAmount(line);
            b.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
            bookings.add(b);
        }
        if (total.signum() <= 0) throw new ValidationException("The total for this booking must be more than zero.");

        String billedTo = customer.getFullName() + " (" + customer.getEmail() + ")";
        if (billedTo.length() > 250) billedTo = billedTo.substring(0, 250);
        Invoice invoice = invoiceService.createInvoice(invoiceService.generateInvoiceNumber(), billedTo,
                LocalDateTime.now(), total);
        for (TourBooking b : bookings) {
            b.setInvoiceId(invoice.getInvoiceId());
            bookingRepository.save(b);
        }
        return new CheckoutResult(invoice, bookings);
    }

    @Transactional(readOnly = true)
    public List<TourBooking> forCustomer(Long customerId) {
        return bookingRepository.findForCustomer(customerId);
    }

    @Transactional(readOnly = true)
    public List<TourBooking> all() {
        return bookingRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<TourBooking> forInvoice(Integer invoiceId) {
        return bookingRepository.findByInvoiceId(invoiceId);
    }

    @Transactional
    public void setCancelled(Integer bookingId, boolean cancelled) {
        TourBooking b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
        b.setStatus(cancelled ? TourBooking.CANCELLED : TourBooking.ACTIVE);
    }

    /** Billing asks this before deleting an invoice. */
    @Override
    @Transactional(readOnly = true)
    public String blockReason(Integer invoiceId) {
        long active = bookingRepository.findByInvoiceId(invoiceId).stream().filter(b -> !b.isCancelled()).count();
        return active > 0 ? "it is linked to " + active + " active tour booking(s). Cancel the booking(s) first." : null;
    }

    private String newReference() {
        String prefix = "BK-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        String ref;
        do {
            ref = prefix + ThreadLocalRandom.current().nextInt(1000, 10000);
        } while (bookingRepository.existsByBookingRef(ref));
        return ref;
    }
}
