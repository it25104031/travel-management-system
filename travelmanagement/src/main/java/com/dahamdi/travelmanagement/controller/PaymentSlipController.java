package com.dahamdi.travelmanagement.controller;

import com.dahamdi.travelmanagement.config.AuthInterceptor;
import com.dahamdi.travelmanagement.entity.Invoice;
import com.dahamdi.travelmanagement.entity.PaymentSlip;
import com.dahamdi.travelmanagement.entity.User;
import com.dahamdi.travelmanagement.exception.NotFoundException;
import com.dahamdi.travelmanagement.repository.InvoiceRepository;
import com.dahamdi.travelmanagement.service.PaymentSlipService;
import com.dahamdi.travelmanagement.service.UserService;
import com.dahamdi.travelmanagement.util.DateUtils;
import jakarta.servlet.http.HttpSession;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Controller
public class PaymentSlipController {

    private static final Set<String> STATUS_FILTERS = Set.of("ALL", "PENDING", "APPROVED", "REJECTED");

    private final PaymentSlipService paymentSlipService;
    private final InvoiceRepository invoiceRepository;
    private final UserService userService;

    public PaymentSlipController(PaymentSlipService paymentSlipService,
                                 InvoiceRepository invoiceRepository,
                                 UserService userService) {
        this.paymentSlipService = paymentSlipService;
        this.invoiceRepository = invoiceRepository;
        this.userService = userService;
    }

    // ==========================================
    // USER ENDPOINTS (login checked by AuthInterceptor)
    // ==========================================

    @GetMapping("/user/payment-slips")
    public String showUserPaymentSlips(HttpSession session, Model model) {
        User sessionUser = (User) session.getAttribute(AuthInterceptor.SESSION_USER);
        User user = currentUser(sessionUser);

        List<PaymentSlip> slips = paymentSlipService.getPaymentSlipsForUser(user);

        model.addAttribute("user", sessionUser);
        model.addAttribute("slips", slips);

        return "user-payment-slips";
    }

    @PostMapping("/user/payment-slips/upload")
    public String processSlipUpload(
            @RequestParam String invoiceNumber,
            @RequestParam BigDecimal amountPaid,
            @RequestParam String paymentDate,
            @RequestParam(required = false) String bankReference,
            @RequestParam("file") MultipartFile file,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User sessionUser = (User) session.getAttribute(AuthInterceptor.SESSION_USER);

        try {
            if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("Invoice number is required.");
            }

            Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Invoice number '" + invoiceNumber.trim()
                            + "' was not found. Please check and try again."));

            paymentSlipService.savePaymentSlip(currentUser(sessionUser), invoice, amountPaid,
                    DateUtils.parseDateTime(paymentDate), bankReference, file);

            redirectAttributes.addFlashAttribute("successMessage", "Payment slip uploaded successfully for invoice "
                    + invoice.getInvoiceNumber() + "! It is now PENDING review.");
        } catch (IllegalArgumentException | IllegalStateException | NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/user/payment-slips";
    }

    // ==========================================
    // ADMIN ENDPOINTS (ADMIN role checked by AuthInterceptor)
    // ==========================================

    @GetMapping("/admin/payment-slips")
    public String showAdminPaymentSlips(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            HttpSession session,
            Model model) {

        String filter = cleanStatusFilter(status);

        model.addAttribute("user", session.getAttribute(AuthInterceptor.SESSION_USER));
        model.addAttribute("slips", paymentSlipService.getAllPaymentSlips(filter));
        model.addAttribute("selectedStatus", filter);

        return "admin-payment-slips";
    }

    @PostMapping("/admin/payment-slips/{id}/approve")
    public String approveSlip(
            @PathVariable Integer id,
            @RequestParam(required = false) String adminRemark,
            @RequestParam(required = false, defaultValue = "ALL") String currentStatus,
            RedirectAttributes redirectAttributes) {

        try {
            paymentSlipService.approveSlip(id, adminRemark);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment slip #" + id + " has been APPROVED and added to Payment Management.");
        } catch (IllegalArgumentException | IllegalStateException | NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/payment-slips?status=" + cleanStatusFilter(currentStatus);
    }

    @PostMapping("/admin/payment-slips/{id}/reject")
    public String rejectSlip(
            @PathVariable Integer id,
            @RequestParam(required = false) String adminRemark,
            @RequestParam(required = false, defaultValue = "ALL") String currentStatus,
            RedirectAttributes redirectAttributes) {

        try {
            paymentSlipService.rejectSlip(id, adminRemark);
            redirectAttributes.addFlashAttribute("successMessage", "Payment slip #" + id + " has been REJECTED.");
        } catch (IllegalArgumentException | IllegalStateException | NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/payment-slips?status=" + cleanStatusFilter(currentStatus);
    }

    // ==========================================
    // SECURE FILE VIEW & DOWNLOAD (ONLY OWNER OR ADMIN)
    // ==========================================

    @GetMapping("/payment-slips/file/{id}")
    public ResponseEntity<Resource> viewSlipFile(@PathVariable Integer id, HttpSession session) {
        return serveFile(id, session, false);
    }

    @GetMapping("/payment-slips/file/{id}/download")
    public ResponseEntity<Resource> downloadSlipFile(@PathVariable Integer id, HttpSession session) {
        return serveFile(id, session, true);
    }

    private ResponseEntity<Resource> serveFile(Integer id, HttpSession session, boolean isDownload) {
        User loggedInUser = (User) session.getAttribute(AuthInterceptor.SESSION_USER);
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PaymentSlip slip = paymentSlipService.getPaymentSlipById(id).orElse(null);
        if (slip == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        boolean isAdmin = "ADMIN".equals(loggedInUser.getRole());
        boolean isOwner = slip.getUser() != null && slip.getUser().getUserId().equals(loggedInUser.getUserId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Resource resource = paymentSlipService.loadFileAsResource(slip);

            MediaType mediaType;
            try {
                mediaType = MediaType.parseMediaType(slip.getContentType());
            } catch (Exception e) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            // ContentDisposition builder escapes the file name safely
            ContentDisposition disposition = (isDownload ? ContentDisposition.attachment() : ContentDisposition.inline())
                    .filename(slip.getOriginalFileName(), StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(resource);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /** Loads the real user from the DB (the session only holds a password-free copy). */
    private User currentUser(User sessionUser) {
        return userService.getUserById(sessionUser.getUserId())
                .orElseThrow(() -> new NotFoundException("Your account no longer exists. Please log in again."));
    }

    private static String cleanStatusFilter(String status) {
        String upper = status == null ? "ALL" : status.trim().toUpperCase();
        return STATUS_FILTERS.contains(upper) ? upper : "ALL";
    }
}
