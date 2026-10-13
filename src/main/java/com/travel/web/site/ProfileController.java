package com.travel.web.site;

import com.travel.common.RequestUtils;
import com.travel.common.ValidationException;
import com.travel.module1_auth.domain.SiteCustomerEntity;
import com.travel.module1_auth.service.SiteCustomerService;
import com.travel.module2_visa.controller.FileController;
import com.travel.module2_visa.domain.ApplicationStatus;
import com.travel.module2_visa.domain.VisaApplicationEntity;
import com.travel.module2_visa.repository.VisaApplicationRepository;
import com.travel.module2_visa.service.DocumentStorageService;
import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.entity.PaymentSlip;
import com.travel.module3_billing.repository.PaymentRepository;
import com.travel.module3_billing.service.InvoiceService;
import com.travel.module3_billing.service.PaymentService;
import com.travel.module3_billing.service.PaymentSlipService;
import com.travel.module3_billing.util.DateUtils;
import com.travel.module5_packages.entity.TourBooking;
import com.travel.module5_packages.service.BookingService;
import com.travel.module5_packages.service.BookingStatusService;
import com.travel.module5_packages.service.BookingStatusService.BookingView;
import com.travel.module5_packages.service.BookingStatusService.InvoiceView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The traveller's profile: trips (bookings), payments (invoices and bank slips; card payments are coming soon),
 * visa applications and account settings. Every page checks the record belongs to the
 * signed-in traveller.
 */
@Controller
@RequestMapping("/my")
public class ProfileController {

    private static final Set<String> TABS = Set.of("trips", "payments", "visas", "settings");

    private final SiteCustomerService customerService;
    private final BookingService bookingService;
    private final BookingStatusService statusService;
    private final InvoiceService invoiceService;
    private final PaymentRepository paymentRepository;
    private final PaymentSlipService slipService;
    private final VisaApplicationRepository visaRepository;
    private final DocumentStorageService storage;

    public ProfileController(SiteCustomerService customerService, BookingService bookingService,
                             BookingStatusService statusService, InvoiceService invoiceService,
                             PaymentRepository paymentRepository, PaymentSlipService slipService,
                             VisaApplicationRepository visaRepository,
                             DocumentStorageService storage) {
        this.customerService = customerService;
        this.bookingService = bookingService;
        this.statusService = statusService;
        this.invoiceService = invoiceService;
        this.paymentRepository = paymentRepository;
        this.slipService = slipService;
        this.visaRepository = visaRepository;
        this.storage = storage;
    }

    /** One invoice row on the Payments tab. */
    public record InvoiceRow(InvoiceView view, List<TourBooking> bookings) { }

    @GetMapping
    public String profile(@RequestParam(required = false, defaultValue = "trips") String tab, Model model) {
        SiteCustomerEntity me = customerService.requireCurrent();
        List<BookingView> trips = statusService.views(bookingService.forCustomer(me.getId()));
        LocalDate today = LocalDate.now();

        List<BookingView> upcoming = trips.stream()
                .filter(v -> !v.booking().isCancelled() && !v.booking().getTravelDate().isBefore(today)).toList();
        List<BookingView> past = trips.stream()
                .filter(v -> v.booking().isCancelled() || v.booking().getTravelDate().isBefore(today)).toList();

        // Payments tab: one row per invoice
        Map<Integer, InvoiceRow> invoices = new LinkedHashMap<>();
        for (BookingView v : trips) {
            if (v.invoice() == null) continue;
            invoices.computeIfAbsent(v.invoice().getInvoiceId(),
                    id -> new InvoiceRow(statusService.invoiceView(v.invoice()), new ArrayList<>())).bookings().add(v.booking());
        }
        BigDecimal due = invoices.values().stream()
                .filter(r -> r.bookings().stream().anyMatch(b -> !b.isCancelled()))
                .map(r -> r.view().outstanding()).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<VisaApplicationEntity> visas = visaRepository
                .findBySiteCustomerIdOrEmailIdIgnoreCaseOrderByCreatedAtDesc(me.getId(), me.getEmail());

        model.addAttribute("tab", TABS.contains(tab) ? tab : "trips");
        model.addAttribute("me", me);
        model.addAttribute("upcoming", upcoming);
        model.addAttribute("past", past);
        model.addAttribute("invoices", invoices.values());
        model.addAttribute("amountDue", due);
        model.addAttribute("slips", slipService.getPaymentSlipsForCustomer(me.getId()));
        model.addAttribute("visas", visas);
        model.addAttribute("openVisas", visas.stream().filter(v -> !v.getStatusEnum().isClosed()).count());
        return "site/profile";
    }

    // ------------------------------------------------------------------ trips

    @PostMapping("/trips/{bookingId}/delete")
    public String deleteTrip(@PathVariable Integer bookingId, RedirectAttributes flash) {
        SiteCustomerEntity me = customerService.requireCurrent();
        try {
            boolean deleted = bookingService.deleteUnpaid(me, bookingId);
            flash.addFlashAttribute("success", deleted ? "Trip deleted." : "Trip cancelled.");
        } catch (ValidationException | IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my?tab=trips";
    }

    // ------------------------------------------------------------------ invoices & payments

    @GetMapping("/invoices/{id}")
    public String invoice(@PathVariable Integer id, Model model) {
        SiteCustomerEntity me = customerService.requireCurrent();
        List<TourBooking> bookings = ownedBookings(id, me);
        Invoice invoice = invoiceService.getInvoiceById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        InvoiceView view = statusService.invoiceView(invoice);

        model.addAttribute("view", view);
        model.addAttribute("bookings", bookings);
        model.addAttribute("payments", paymentRepository.findByInvoice_InvoiceIdOrderByPaymentDateDesc(id));
        model.addAttribute("slips", slipService.getPaymentSlipsForInvoice(id));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("allCancelled", bookings.stream().allMatch(TourBooking::isCancelled));
        return "site/invoice";
    }

    @PostMapping("/invoices/{id}/slip")
    public String uploadSlip(@PathVariable Integer id,
                             @RequestParam(required = false) BigDecimal amountPaid,
                             @RequestParam(required = false) String paymentDate,
                             @RequestParam(required = false) String bankReference,
                             @RequestParam(value = "file", required = false) MultipartFile file,
                             RedirectAttributes flash) {
        SiteCustomerEntity me = customerService.requireCurrent();
        List<TourBooking> bookings = ownedBookings(id, me);
        if (bookings.stream().allMatch(TourBooking::isCancelled)) {
            flash.addFlashAttribute("error", "This booking was cancelled, so it can't be paid.");
            return "redirect:/my/invoices/" + id;
        }
        try {
            Invoice invoice = invoiceService.getInvoiceOrThrow(id);
            slipService.saveCustomerPaymentSlip(me, invoice, amountPaid, DateUtils.parseDateTime(paymentDate), bankReference, file);
            flash.addFlashAttribute("success", "Bank slip received. Our team will check it within one working day and confirm your booking.");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("error", e.getMessage());
            flash.addFlashAttribute("payTab", "bank");
        }
        return "redirect:/my/invoices/" + id;
    }

    @GetMapping("/slips/{id}/file")
    public ResponseEntity<Resource> slipFile(@PathVariable Integer id) {
        SiteCustomerEntity me = customerService.requireCurrent();
        PaymentSlip slip = slipService.getPaymentSlipById(id).orElse(null);
        if (slip == null || slip.getCustomer() == null || !slip.getCustomer().getId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Resource resource = slipService.loadFileAsResource(slip);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(slip.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(slip.getOriginalFileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    // ------------------------------------------------------------------ visas

    @GetMapping("/visas/{id}/approved-visa")
    public ResponseEntity<Resource> approvedVisa(@PathVariable Long id) {
        SiteCustomerEntity me = customerService.requireCurrent();
        VisaApplicationEntity app = visaRepository.findById(id).orElse(null);
        boolean mine = app != null && (me.getId().equals(app.getSiteCustomerId())
                || (app.getEmailId() != null && app.getEmailId().equalsIgnoreCase(me.getEmail())));
        if (!mine || app.getStatusEnum() != ApplicationStatus.APPROVED || app.getApprovedVisaPath() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return FileController.serve(storage, app.getApprovedVisaPath(), "application/pdf", "visa-" + id + ".pdf");
    }

    // ------------------------------------------------------------------ settings

    @PostMapping("/settings")
    public String updateSettings(@RequestParam String fullName, @RequestParam(required = false) String phone,
                                 HttpServletRequest request, RedirectAttributes flash) {
        SiteCustomerEntity me = customerService.requireCurrent();
        try {
            customerService.updateProfile(me.getId(), fullName, phone, RequestUtils.clientIp(request));
            flash.addFlashAttribute("success", "Your details were saved.");
        } catch (ValidationException e) {
            flash.addFlashAttribute("errors", e.getErrors());
        }
        return "redirect:/my?tab=settings";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam String currentPassword, @RequestParam String newPassword,
                                 @RequestParam String confirmPassword, HttpServletRequest request,
                                 RedirectAttributes flash) {
        SiteCustomerEntity me = customerService.requireCurrent();
        try {
            customerService.changePassword(me.getId(), currentPassword, newPassword, confirmPassword, RequestUtils.clientIp(request));
            flash.addFlashAttribute("success", "Your password was changed.");
        } catch (ValidationException e) {
            flash.addFlashAttribute("errors", e.getErrors());
        }
        return "redirect:/my?tab=settings";
    }

    // ------------------------------------------------------------------ helpers

    /** The bookings on this invoice, but only if they belong to the signed-in traveller (else 404). */
    private List<TourBooking> ownedBookings(Integer invoiceId, SiteCustomerEntity me) {
        List<TourBooking> bookings = bookingService.forInvoice(invoiceId);
        if (bookings.isEmpty() || bookings.stream().anyMatch(b -> !b.getCustomer().getId().equals(me.getId()))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return bookings;
    }
}
