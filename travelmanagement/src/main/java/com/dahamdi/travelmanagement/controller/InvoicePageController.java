package com.dahamdi.travelmanagement.controller;

import com.dahamdi.travelmanagement.airline.Booking;
import com.dahamdi.travelmanagement.airline.BookingRepository;
import com.dahamdi.travelmanagement.entity.Invoice;
import com.dahamdi.travelmanagement.repository.InvoiceRepository;
import com.dahamdi.travelmanagement.service.InvoiceService;
import com.dahamdi.travelmanagement.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class InvoicePageController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;

    public InvoicePageController(
            InvoiceService invoiceService,
            PaymentService paymentService,
            BookingRepository bookingRepository,
            InvoiceRepository invoiceRepository) {

        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.bookingRepository = bookingRepository;
        this.invoiceRepository = invoiceRepository;
    }

    // ============================================================
    // Display all invoices
    // ============================================================

    @GetMapping("/invoices")
    public String showInvoicesPage(Model model) {

        List<Invoice> invoices = invoiceService.getAllInvoices();

        Map<Integer, Double> paidAmounts = new HashMap<>();
        Map<Integer, Double> outstandingBalances = new HashMap<>();
        Map<Integer, String> calculatedStatuses = new HashMap<>();

        for (Invoice invoice : invoices) {

            Integer invoiceId = invoice.getInvoiceId();

            Double paidAmount =
                    paymentService.getCompletedPaymentTotal(invoiceId);

            Double outstandingBalance =
                    paymentService.getOutstandingBalance(invoiceId);

            String calculatedStatus =
                    paymentService.getInvoiceStatus(invoiceId);

            paidAmounts.put(invoiceId, paidAmount);
            outstandingBalances.put(invoiceId, outstandingBalance);
            calculatedStatuses.put(invoiceId, calculatedStatus);
        }

        List<Booking> bookings = bookingRepository.findAll();

        model.addAttribute("invoices", invoices);
        model.addAttribute("paidAmounts", paidAmounts);
        model.addAttribute("outstandingBalances", outstandingBalances);
        model.addAttribute("calculatedStatuses", calculatedStatuses);
        model.addAttribute("bookings", bookings);

        return "invoices";
    }

    // ============================================================
    // Add a new invoice
    // ============================================================

    @PostMapping("/invoices/add")
    public String addInvoice(
            @RequestParam String invoiceNumber,
            @RequestParam String invoiceDate,
            @RequestParam String invoiceStatus,
            @RequestParam(required = false) Integer bookingId,
            Model model) {

        if (bookingId == null) {

            model.addAttribute(
                    "errorMessage",
                    "Please select a booking before creating an invoice."
            );

            loadInvoicePageData(model);

            return "invoices";
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException("Booking not found"));

        // Check whether this booking already has an invoice
        if (invoiceRepository.findByBooking_Id(bookingId).isPresent()) {

            model.addAttribute(
                    "errorMessage",
                    "This booking already has an invoice."
            );

            loadInvoicePageData(model);

            return "invoices";
        }

        Invoice invoice = new Invoice();

        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceDate(LocalDateTime.parse(invoiceDate));
        invoice.setInvoiceStatus(invoiceStatus);

        // Get customer and total amount from the booking
        invoice.setBooking(booking);
        invoice.setCustomerName(booking.getCustomerName());
        invoice.setTotalAmount(
                booking.getTotalAmount().doubleValue()
        );

        invoiceService.createInvoice(invoice);

        return "redirect:/invoices";
    }

    // ============================================================
    // Display the edit invoice page
    // ============================================================

    @GetMapping("/invoices/edit/{id}")
    public String showEditInvoicePage(
            @PathVariable Integer id,
            Model model) {

        Invoice invoice = invoiceService.getInvoiceById(id)
                .orElseThrow(() ->
                        new RuntimeException("Invoice not found"));

        List<Booking> bookings = bookingRepository.findAll();

        model.addAttribute("invoice", invoice);
        model.addAttribute("bookings", bookings);

        return "edit-invoice";
    }

    // ============================================================
    // Update an existing invoice
    // ============================================================

    @PostMapping("/invoices/update/{id}")
    public String updateInvoice(
            @PathVariable Integer id,
            @RequestParam String invoiceNumber,
            @RequestParam String invoiceDate,
            @RequestParam String invoiceStatus,
            @RequestParam(required = false) Integer bookingId,
            Model model) {

        Invoice invoice = invoiceService.getInvoiceById(id)
                .orElseThrow(() ->
                        new RuntimeException("Invoice not found"));

        if (bookingId == null) {

            model.addAttribute(
                    "errorMessage",
                    "Please select a booking."
            );

            model.addAttribute("invoice", invoice);
            model.addAttribute(
                    "bookings",
                    bookingRepository.findAll()
            );

            return "edit-invoice";
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new RuntimeException("Booking not found"));

        var existingInvoice =
                invoiceRepository.findByBooking_Id(bookingId);

        if (existingInvoice.isPresent()
                && !existingInvoice.get().getInvoiceId()
                .equals(invoice.getInvoiceId())) {

            model.addAttribute(
                    "errorMessage",
                    "This booking already has another invoice."
            );

            model.addAttribute("invoice", invoice);
            model.addAttribute(
                    "bookings",
                    bookingRepository.findAll()
            );

            return "edit-invoice";
        }

        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceDate(LocalDateTime.parse(invoiceDate));
        invoice.setInvoiceStatus(invoiceStatus);

        // Get customer and amount from the booking
        invoice.setBooking(booking);
        invoice.setCustomerName(booking.getCustomerName());
        invoice.setTotalAmount(
                booking.getTotalAmount().doubleValue()
        );

        invoiceService.createInvoice(invoice);

        return "redirect:/invoices";
    }

    // ============================================================
    // Print invoice
    // ============================================================

    @GetMapping("/invoices/print/{id}")
    public String printInvoice(
            @PathVariable Integer id,
            Model model) {

        Invoice invoice = invoiceService.getInvoiceById(id)
                .orElseThrow(() ->
                        new RuntimeException("Invoice not found"));

        Integer invoiceId = invoice.getInvoiceId();

        // Calculate payment information
        Double paidAmount =
                paymentService.getCompletedPaymentTotal(invoiceId);

        Double outstandingBalance =
                paymentService.getOutstandingBalance(invoiceId);

        String calculatedStatus =
                paymentService.getInvoiceStatus(invoiceId);

        // Send invoice information to the printable page
        model.addAttribute("invoice", invoice);
        model.addAttribute("paidAmount", paidAmount);
        model.addAttribute(
                "outstandingBalance",
                outstandingBalance
        );
        model.addAttribute(
                "calculatedStatus",
                calculatedStatus
        );

        return "print-invoice";
    }

    // ============================================================
    // Delete an invoice
    // ============================================================

    @PostMapping("/invoices/delete/{id}")
    public String deleteInvoice(@PathVariable Integer id) {

        invoiceService.deleteInvoice(id);

        return "redirect:/invoices";
    }

    // ============================================================
    // Load data needed by invoices.html
    // when validation fails
    // ============================================================

    private void loadInvoicePageData(Model model) {

        List<Invoice> invoices = invoiceService.getAllInvoices();

        Map<Integer, Double> paidAmounts = new HashMap<>();
        Map<Integer, Double> outstandingBalances = new HashMap<>();
        Map<Integer, String> calculatedStatuses = new HashMap<>();

        for (Invoice invoice : invoices) {

            Integer invoiceId = invoice.getInvoiceId();

            Double paidAmount =
                    paymentService.getCompletedPaymentTotal(invoiceId);

            Double outstandingBalance =
                    paymentService.getOutstandingBalance(invoiceId);

            String calculatedStatus =
                    paymentService.getInvoiceStatus(invoiceId);

            paidAmounts.put(invoiceId, paidAmount);
            outstandingBalances.put(
                    invoiceId,
                    outstandingBalance
            );
            calculatedStatuses.put(
                    invoiceId,
                    calculatedStatus
            );
        }

        List<Booking> bookings = bookingRepository.findAll();

        model.addAttribute("invoices", invoices);
        model.addAttribute(
                "paidAmounts",
                paidAmounts
        );
        model.addAttribute(
                "outstandingBalances",
                outstandingBalances
        );
        model.addAttribute(
                "calculatedStatuses",
                calculatedStatuses
        );
        model.addAttribute(
                "bookings",
                bookings
        );
    }
}