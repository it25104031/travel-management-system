package com.travel.module5_packages.entity;

import com.travel.module1_auth.domain.SiteCustomerEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A tour package booked by a website traveller at checkout. One checkout creates one invoice
 * (Module 3) and one booking per cart item; the booking keeps the invoice id so payment
 * progress (UNPAID / PARTIAL / PAID) always comes from Billing.
 */
@Entity
@Table(name = "tour_booking")
public class TourBooking {

    public static final String ACTIVE = "ACTIVE";
    public static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Integer bookingId;

    @Column(name = "booking_ref", nullable = false, unique = true, length = 20)
    private String bookingRef;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private SiteCustomerEntity customer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private TourPackage tourPackage;

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @Column(nullable = false)
    private Integer travellers;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "invoice_id")
    private Integer invoiceId;

    @Column(nullable = false, length = 20)
    private String status = ACTIVE;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = ACTIVE;
    }

    public Integer getBookingId() { return bookingId; }
    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }
    public SiteCustomerEntity getCustomer() { return customer; }
    public void setCustomer(SiteCustomerEntity customer) { this.customer = customer; }
    public TourPackage getTourPackage() { return tourPackage; }
    public void setTourPackage(TourPackage tourPackage) { this.tourPackage = tourPackage; }
    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }
    public Integer getTravellers() { return travellers; }
    public void setTravellers(Integer travellers) { this.travellers = travellers; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Integer getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Integer invoiceId) { this.invoiceId = invoiceId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isCancelled() { return CANCELLED.equals(status); }

    public LocalDate getReturnDate() {
        int days = tourPackage == null || tourPackage.getDuration() == null ? 1 : tourPackage.getDuration();
        return travelDate == null ? null : travelDate.plusDays(Math.max(0, days - 1));
    }
}
