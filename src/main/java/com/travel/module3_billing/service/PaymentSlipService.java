package com.travel.module3_billing.service;

import com.travel.module3_billing.entity.Invoice;
import com.travel.module3_billing.entity.PaymentSlip;
import com.travel.module1_auth.domain.AdminUserEntity;
import com.travel.module3_billing.exception.NotFoundException;
import com.travel.module3_billing.repository.PaymentSlipRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentSlipService {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024;

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png",
            ".pdf", "application/pdf"
    );

    private final PaymentSlipRepository paymentSlipRepository;
    private final PaymentService paymentService;
    private final Path uploadLocation;

    public PaymentSlipService(
            PaymentSlipRepository paymentSlipRepository,
            PaymentService paymentService,
            @Value("${file.upload-dir:uploads/payment-slips}") String uploadDir) {
        this.paymentSlipRepository = paymentSlipRepository;
        this.paymentService = paymentService;
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.uploadLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory at " + this.uploadLocation, e);
        }
    }

    @Transactional
    public PaymentSlip saveCustomerPaymentSlip(
            com.travel.module1_auth.domain.SiteCustomerEntity customer,
            Invoice invoice,
            BigDecimal amountPaid,
            LocalDateTime paymentDate,
            String bankReference,
            MultipartFile file) {
        if (customer == null) {
            throw new IllegalArgumentException("Please sign in to upload a payment slip.");
        }
        if (paymentSlipRepository.existsByInvoice_InvoiceIdAndStatus(invoice.getInvoiceId(), PENDING)) {
            throw new IllegalArgumentException("A slip for invoice " + invoice.getInvoiceNumber()
                    + " is already waiting for review. We'll update your booking as soon as it is checked.");
        }
        PaymentSlip slip = savePaymentSlip(null, invoice, amountPaid, paymentDate, bankReference, file);
        slip.setCustomer(customer);
        return paymentSlipRepository.save(slip);
    }

    public List<PaymentSlip> getPaymentSlipsForCustomer(Long customerId) {
        return paymentSlipRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId);
    }

    public List<PaymentSlip> getPaymentSlipsForInvoice(Integer invoiceId) {
        return paymentSlipRepository.findByInvoice_InvoiceIdOrderByCreatedAtDesc(invoiceId);
    }

    public boolean hasPendingSlip(Integer invoiceId) {
        return paymentSlipRepository.existsByInvoice_InvoiceIdAndStatus(invoiceId, PENDING);
    }

    @Transactional
    public PaymentSlip savePaymentSlip(
            AdminUserEntity user,
            Invoice invoice,
            BigDecimal amountPaid,
            LocalDateTime paymentDate,
            String bankReference,
            MultipartFile file) {

        if (amountPaid == null || amountPaid.signum() <= 0) {
            throw new IllegalArgumentException("Amount paid must be greater than 0.");
        }
        if (paymentDate == null) {
            throw new IllegalArgumentException("Payment date is required.");
        }
        if (paymentDate.isAfter(LocalDateTime.now().plusDays(1))) {
            throw new IllegalArgumentException("Payment date can't be in the future.");
        }

        BigDecimal outstanding = paymentService.getOutstandingBalance(invoice.getInvoiceId());
        if (outstanding.signum() <= 0) {
            throw new IllegalArgumentException("Invoice " + invoice.getInvoiceNumber() + " is already fully paid.");
        }
        if (amountPaid.compareTo(outstanding) > 0) {
            throw new IllegalArgumentException("Amount paid can't be more than the outstanding balance of LKR "
                    + PaymentService.formatMoney(outstanding) + ".");
        }
        if (bankReference != null && bankReference.trim().length() > 100) {
            throw new IllegalArgumentException("Bank reference can be at most 100 characters.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a file to upload.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of 5 MB.");
        }

        String originalFileName = cleanFileName(file.getOriginalFilename());

        String ext = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalFileName.substring(dotIndex).toLowerCase();
        }

        String contentType = ALLOWED_TYPES.get(ext);
        if (contentType == null) {
            throw new IllegalArgumentException("Only JPG, PNG, and PDF files are allowed.");
        }
        if (!fileContentMatches(file, ext)) {
            throw new IllegalArgumentException("The file content doesn't match its extension. Please upload a real JPG, PNG or PDF.");
        }

        String storedFileName = UUID.randomUUID() + ext;
        Path targetPath = this.uploadLocation.resolve(storedFileName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store payment slip file.", e);
        }

        PaymentSlip slip = new PaymentSlip();
        slip.setUser(user);
        slip.setInvoice(invoice);
        slip.setAmountPaid(amountPaid);
        slip.setPaymentDate(paymentDate);
        slip.setBankReference(bankReference != null ? bankReference.trim() : "");
        slip.setOriginalFileName(originalFileName);
        slip.setStoredFileName(storedFileName);
        slip.setContentType(contentType);
        slip.setFileSize(file.getSize());
        slip.setStatus(PENDING);
        slip.setCreatedAt(LocalDateTime.now());

        return paymentSlipRepository.save(slip);
    }

    public List<PaymentSlip> getPaymentSlipsForUser(AdminUserEntity user) {
        return paymentSlipRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<PaymentSlip> getAllPaymentSlips(String statusFilter) {
        if (statusFilter == null || statusFilter.trim().isEmpty() || "ALL".equalsIgnoreCase(statusFilter)) {
            return paymentSlipRepository.findAllByOrderByCreatedAtDesc();
        }
        return paymentSlipRepository.findByStatusOrderByCreatedAtDesc(statusFilter.trim().toUpperCase());
    }

    public Optional<PaymentSlip> getPaymentSlipById(Integer slipId) {
        return paymentSlipRepository.findById(slipId);
    }

    public Resource loadFileAsResource(PaymentSlip slip) {
        try {
            Path filePath = this.uploadLocation.resolve(slip.getStoredFileName()).normalize();
            if (!filePath.startsWith(this.uploadLocation)) {
                throw new NotFoundException("File not found.");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new NotFoundException("File not found or not readable: " + slip.getOriginalFileName());
        } catch (MalformedURLException e) {
            throw new NotFoundException("File path invalid: " + slip.getOriginalFileName());
        }
    }

    @Transactional
    public void approveSlip(Integer slipId, String adminRemark) {
        PaymentSlip slip = getPendingSlip(slipId);

        Invoice invoice = slip.getInvoice();
        if (invoice == null) {
            throw new IllegalStateException("Payment slip #" + slipId + " is not linked to an invoice.");
        }

        BigDecimal outstanding = paymentService.getOutstandingBalance(invoice.getInvoiceId());
        if (slip.getAmountPaid().compareTo(outstanding) > 0) {
            throw new IllegalStateException("Slip #" + slipId + " is for LKR " + PaymentService.formatMoney(slip.getAmountPaid())
                    + " but invoice " + invoice.getInvoiceNumber() + " only has LKR "
                    + PaymentService.formatMoney(outstanding) + " outstanding. Reject it instead.");
        }

        paymentService.createPayment(
                invoice.getInvoiceId(),
                slip.getPaymentDate() != null ? slip.getPaymentDate() : LocalDateTime.now(),
                slip.getAmountPaid(),
                "SLIP_TRANSFER",
                PaymentService.STATUS_COMPLETED);

        slip.setStatus(APPROVED);
        slip.setAdminRemark(cleanRemark(adminRemark));
        paymentSlipRepository.save(slip);
    }

    @Transactional
    public void rejectSlip(Integer slipId, String adminRemark) {
        PaymentSlip slip = getPendingSlip(slipId);

        slip.setStatus(REJECTED);
        slip.setAdminRemark(cleanRemark(adminRemark));
        paymentSlipRepository.save(slip);
    }

    private PaymentSlip getPendingSlip(Integer slipId) {
        PaymentSlip slip = paymentSlipRepository.findById(slipId)
                .orElseThrow(() -> new NotFoundException("Payment slip #" + slipId + " was not found."));

        if (!PENDING.equals(slip.getStatus())) {
            throw new IllegalStateException("Payment slip #" + slipId + " has already been "
                    + slip.getStatus() + " and can't be changed again.");
        }
        return slip;
    }

    private static String cleanRemark(String remark) {
        return (remark == null || remark.isBlank()) ? null : remark.trim();
    }

    private static String cleanFileName(String name) {
        if (name == null || name.isBlank()) {
            return "uploaded_slip";
        }
        String justName = Paths.get(name.replace('\\', '/')).getFileName().toString();
        String cleaned = justName.replaceAll("[\\r\\n\"\\\\]", "_");
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(cleaned.length() - 200);
        }
        return cleaned;
    }

    private static boolean fileContentMatches(MultipartFile file, String ext) {
        byte[] head = new byte[8];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            return false;
        }
        if (read < 4) {
            return false;
        }
        return switch (ext) {
            case ".png" -> (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
            case ".jpg", ".jpeg" -> (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8;
            case ".pdf" -> head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F';
            default -> false;
        };
    }
}
