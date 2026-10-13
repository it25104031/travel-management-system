package com.travel.module4_logistics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "supplier_payment")
public class SupplierPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_payment_id")
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_id", nullable = false)
    private Departure departure;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 60)
    private String reference;

    protected SupplierPayment() { }

    public SupplierPayment(Supplier supplier, Departure departure, LocalDate date, BigDecimal amount, String reference) {
        this.supplier = supplier;
        this.departure = departure;
        this.paymentDate = date;
        this.amount = amount;
        this.reference = reference;
    }

    public void updateDetails(Supplier supplier, Departure departure, LocalDate date, BigDecimal amount, String reference) {
        this.supplier = supplier;
        this.departure = departure;
        this.paymentDate = date;
        this.amount = amount;
        this.reference = reference;
    }

    public Integer getId() { return id; }
    public Supplier getSupplier() { return supplier; }
    public Departure getDeparture() { return departure; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public BigDecimal getAmount() { return amount; }
    public String getReference() { return reference; }
}
