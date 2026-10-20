package com.travel.module3_billing.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_slip")
public class PaymentSlip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer slipId;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private com.travel.module1_auth.domain.AdminUserEntity user;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private com.travel.module1_auth.domain.SiteCustomerEntity customer;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @Column
    private String bankReference;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String adminRemark;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PaymentSlip() {
    }

    public Integer getSlipId() {
        return slipId;
    }

    public void setSlipId(Integer slipId) {
        this.slipId = slipId;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public com.travel.module1_auth.domain.AdminUserEntity getUser() {
        return user;
    }

    public void setUser(com.travel.module1_auth.domain.AdminUserEntity user) {
        this.user = user;
    }

    public com.travel.module1_auth.domain.SiteCustomerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(com.travel.module1_auth.domain.SiteCustomerEntity customer) {
        this.customer = customer;
    }

    public String getUploaderName() {
        if (customer != null) return customer.getFullName() + " (traveller)";
        if (user != null) return user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getUsername();
        return "-";
    }

    public String getUploaderEmail() {
        if (customer != null) return customer.getEmail();
        if (user != null) return user.getEmail();
        return null;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getBankReference() {
        return bankReference;
    }

    public void setBankReference(String bankReference) {
        this.bankReference = bankReference;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAdminRemark() {
        return adminRemark;
    }

    public void setAdminRemark(String adminRemark) {
        this.adminRemark = adminRemark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
