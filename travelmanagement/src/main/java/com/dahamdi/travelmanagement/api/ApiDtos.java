package com.dahamdi.travelmanagement.api;

import com.dahamdi.travelmanagement.entity.Invoice;
import com.dahamdi.travelmanagement.entity.Payment;
import com.dahamdi.travelmanagement.entity.PaymentSlip;
import com.dahamdi.travelmanagement.entity.User;
import com.dahamdi.travelmanagement.service.PaymentService.InvoiceSummary;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JSON shapes sent to the Vite frontend.
 * Entities are never returned directly where they could leak data
 * (e.g. PaymentSlip -> User -> password hash).
 */
public final class ApiDtos {

    private ApiDtos() {
    }

    public record UserDto(Integer userId, String username, String fullName, String email, String role) {
        public static UserDto of(User u) {
            return new UserDto(u.getUserId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole());
        }
    }

    public record InvoiceDto(Integer invoiceId, String invoiceNumber, String customerName,
                             LocalDateTime invoiceDate, BigDecimal totalAmount,
                             BigDecimal paidAmount, BigDecimal outstandingBalance, String status) {
        public static InvoiceDto of(Invoice i, InvoiceSummary s) {
            return new InvoiceDto(i.getInvoiceId(), i.getInvoiceNumber(), i.getCustomerName(),
                    i.getInvoiceDate(), i.getTotalAmount(),
                    s.paidAmount(), s.outstandingBalance(), s.status());
        }
    }

    public record PaymentDto(Integer paymentId, Integer invoiceId, String invoiceNumber, String customerName,
                             LocalDateTime paymentDate, BigDecimal amount,
                             String paymentMethod, String paymentStatus) {
        public static PaymentDto of(Payment p) {
            Invoice i = p.getInvoice();
            return new PaymentDto(p.getPaymentId(),
                    i != null ? i.getInvoiceId() : null,
                    i != null ? i.getInvoiceNumber() : null,
                    i != null ? i.getCustomerName() : null,
                    p.getPaymentDate(), p.getAmount(), p.getPaymentMethod(), p.getPaymentStatus());
        }
    }

    public record SlipDto(Integer slipId, Integer invoiceId, String invoiceNumber,
                          String uploadedBy, String uploaderUsername,
                          BigDecimal amountPaid, LocalDateTime paymentDate, String bankReference,
                          String originalFileName, String contentType, Long fileSize,
                          String status, String adminRemark, LocalDateTime createdAt,
                          String fileUrl, String downloadUrl) {
        public static SlipDto of(PaymentSlip s) {
            Invoice i = s.getInvoice();
            User u = s.getUser();
            return new SlipDto(s.getSlipId(),
                    i != null ? i.getInvoiceId() : null,
                    i != null ? i.getInvoiceNumber() : null,
                    u != null ? u.getFullName() : null,
                    u != null ? u.getUsername() : null,
                    s.getAmountPaid(), s.getPaymentDate(), s.getBankReference(),
                    s.getOriginalFileName(), s.getContentType(), s.getFileSize(),
                    s.getStatus(), s.getAdminRemark(), s.getCreatedAt(),
                    "/payment-slips/file/" + s.getSlipId(),
                    "/payment-slips/file/" + s.getSlipId() + "/download");
        }
    }

    // ---- request bodies ----

    public record LoginRequest(String username, String password) {
    }

    public record RegisterRequest(String username, String password, String fullName, String email) {
    }

    public record InvoiceRequest(String invoiceNumber, String customerName,
                                 String invoiceDate, BigDecimal totalAmount) {
    }

    public record PaymentRequest(Integer invoiceId, String paymentDate, BigDecimal amount,
                                 String paymentMethod, String paymentStatus) {
    }

    public record GatewayRequest(Integer invoiceId, String customerName, BigDecimal amount, String paymentMethod) {
    }

    public record RemarkRequest(String adminRemark) {
    }
}
