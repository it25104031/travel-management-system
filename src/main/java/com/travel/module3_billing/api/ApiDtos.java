package com.travel.module3_billing.api;

import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.entity.Payment;
import com.travel.module3_billing.entity.PaymentSlip;
import com.travel.module1_auth.domain.AdminUserEntity;
import com.travel.module3_billing.service.PaymentService.InvoiceSummary;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record UserDto(Long userId, String username, String fullName, String email, String role) {
        public static UserDto of(AdminUserEntity u) {
            return new UserDto(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole());
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
            AdminUserEntity u = s.getUser();
            return new SlipDto(s.getSlipId(),
                    i != null ? i.getInvoiceId() : null,
                    i != null ? i.getInvoiceNumber() : null,
                    s.getUploaderName(),
                    u != null ? u.getUsername() : (s.getCustomer() != null ? s.getCustomer().getEmail() : null),
                    s.getAmountPaid(), s.getPaymentDate(), s.getBankReference(),
                    s.getOriginalFileName(), s.getContentType(), s.getFileSize(),
                    s.getStatus(), s.getAdminRemark(), s.getCreatedAt(),
                    "/billing/slips/file/" + s.getSlipId(),
                    "/billing/slips/file/" + s.getSlipId() + "/download");
        }
    }

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

    public record RemarkRequest(String adminRemark) {
    }
}
