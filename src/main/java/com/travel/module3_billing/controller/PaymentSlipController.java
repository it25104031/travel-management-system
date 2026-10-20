package com.travel.module3_billing.controller;

import com.travel.module1_auth.domain.AdminUserEntity;
import com.travel.module3_billing.config.CurrentStaff;
import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.entity.PaymentSlip;
import com.travel.module3_billing.exception.NotFoundException;
import com.travel.module3_billing.repository.InvoiceRepository;
import com.travel.module3_billing.service.PaymentSlipService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Controller
public class PaymentSlipController {

    private static final Set<String> STATUS_FILTERS = Set.of("ALL", "PENDING", "APPROVED", "REJECTED");

    private final PaymentSlipService paymentSlipService;
    private final InvoiceRepository invoiceRepository;
    private final CurrentStaff currentStaff;

    public PaymentSlipController(PaymentSlipService paymentSlipService,
                                 InvoiceRepository invoiceRepository,
                                 CurrentStaff currentStaff) {
        this.paymentSlipService = paymentSlipService;
        this.invoiceRepository = invoiceRepository;
        this.currentStaff = currentStaff;
    }

    @GetMapping("/billing/slips")
    public String showAdminPaymentSlips(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            Model model) {

        String filter = cleanStatusFilter(status);

        model.addAttribute("user", currentStaff.view());
        model.addAttribute("slips", paymentSlipService.getAllPaymentSlips(filter));
        model.addAttribute("selectedStatus", filter);

        return "module3_billing/admin-payment-slips";
    }

    @PostMapping("/billing/slips/{id}/approve")
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

        return "redirect:/billing/slips?status=" + cleanStatusFilter(currentStatus);
    }

    @PostMapping("/billing/slips/{id}/reject")
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

        return "redirect:/billing/slips?status=" + cleanStatusFilter(currentStatus);
    }

    @GetMapping("/billing/slips/file/{id}")
    public ResponseEntity<Resource> viewSlipFile(@PathVariable Integer id) {
        return serveFile(id, false);
    }

    @GetMapping("/billing/slips/file/{id}/download")
    public ResponseEntity<Resource> downloadSlipFile(@PathVariable Integer id) {
        return serveFile(id, true);
    }

    private ResponseEntity<Resource> serveFile(Integer id, boolean isDownload) {
        AdminUserEntity loggedInUser = currentStaff.get();
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PaymentSlip slip = paymentSlipService.getPaymentSlipById(id).orElse(null);
        if (slip == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            Resource resource = paymentSlipService.loadFileAsResource(slip);

            MediaType mediaType;
            try {
                mediaType = MediaType.parseMediaType(slip.getContentType());
            } catch (Exception e) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

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

    private static String cleanStatusFilter(String status) {
        String upper = status == null ? "ALL" : status.trim().toUpperCase();
        return STATUS_FILTERS.contains(upper) ? upper : "ALL";
    }
}
