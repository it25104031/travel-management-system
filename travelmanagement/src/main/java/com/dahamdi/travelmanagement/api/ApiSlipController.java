package com.dahamdi.travelmanagement.api;

import com.dahamdi.travelmanagement.api.ApiDtos.RemarkRequest;
import com.dahamdi.travelmanagement.api.ApiDtos.SlipDto;
import com.dahamdi.travelmanagement.config.AuthInterceptor;
import com.dahamdi.travelmanagement.entity.Invoice;
import com.dahamdi.travelmanagement.entity.User;
import com.dahamdi.travelmanagement.exception.NotFoundException;
import com.dahamdi.travelmanagement.repository.InvoiceRepository;
import com.dahamdi.travelmanagement.service.PaymentSlipService;
import com.dahamdi.travelmanagement.service.UserService;
import com.dahamdi.travelmanagement.util.DateUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payment slips for the Vite frontend.
 *  /api/my/slips          -> any logged-in user (their own slips, upload)
 *  /api/admin/slips/...   -> ADMIN only (review, approve, reject)
 */
@RestController
public class ApiSlipController {

    private final PaymentSlipService paymentSlipService;
    private final InvoiceRepository invoiceRepository;
    private final UserService userService;

    public ApiSlipController(PaymentSlipService paymentSlipService, InvoiceRepository invoiceRepository,
                             UserService userService) {
        this.paymentSlipService = paymentSlipService;
        this.invoiceRepository = invoiceRepository;
        this.userService = userService;
    }

    @GetMapping("/api/my/slips")
    public List<SlipDto> mySlips(HttpSession session) {
        return paymentSlipService.getPaymentSlipsForUser(currentUser(session)).stream()
                .map(SlipDto::of)
                .toList();
    }

    @PostMapping("/api/my/slips")
    public ResponseEntity<SlipDto> upload(
            @RequestParam String invoiceNumber,
            @RequestParam BigDecimal amountPaid,
            @RequestParam String paymentDate,
            @RequestParam(required = false) String bankReference,
            @RequestParam("file") MultipartFile file,
            HttpSession session) {

        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required.");
        }
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invoice number '" + invoiceNumber.trim()
                        + "' was not found. Check the number on your invoice."));

        var slip = paymentSlipService.savePaymentSlip(currentUser(session), invoice, amountPaid,
                DateUtils.parseDateTime(paymentDate), bankReference, file);

        return ResponseEntity.status(HttpStatus.CREATED).body(SlipDto.of(slip));
    }

    @GetMapping("/api/admin/slips")
    public List<SlipDto> allSlips(@RequestParam(required = false, defaultValue = "ALL") String status) {
        return paymentSlipService.getAllPaymentSlips(status).stream()
                .map(SlipDto::of)
                .toList();
    }

    @PostMapping("/api/admin/slips/{id}/approve")
    public SlipDto approve(@PathVariable Integer id, @RequestBody(required = false) RemarkRequest body) {
        paymentSlipService.approveSlip(id, body != null ? body.adminRemark() : null);
        return SlipDto.of(getSlip(id));
    }

    @PostMapping("/api/admin/slips/{id}/reject")
    public SlipDto reject(@PathVariable Integer id, @RequestBody(required = false) RemarkRequest body) {
        paymentSlipService.rejectSlip(id, body != null ? body.adminRemark() : null);
        return SlipDto.of(getSlip(id));
    }

    private com.dahamdi.travelmanagement.entity.PaymentSlip getSlip(Integer id) {
        return paymentSlipService.getPaymentSlipById(id)
                .orElseThrow(() -> new NotFoundException("Payment slip #" + id + " was not found."));
    }

    private User currentUser(HttpSession session) {
        User sessionUser = (User) session.getAttribute(AuthInterceptor.SESSION_USER);
        return userService.getUserById(sessionUser.getUserId())
                .orElseThrow(() -> new NotFoundException("Your account no longer exists. Please log in again."));
    }
}
