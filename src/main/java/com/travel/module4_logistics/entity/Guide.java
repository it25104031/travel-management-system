package com.travel.module4_logistics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "guide")
public class Guide {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guide_id")
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;

    protected Guide() { }

    public Guide(Supplier supplier, String name, String role, String phone, BigDecimal dailyRate) {
        this.supplier = supplier;
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.dailyRate = dailyRate;
    }

    public void updateDetails(Supplier supplier, String name, String role, String phone, BigDecimal dailyRate) {
        this.supplier = supplier;
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.dailyRate = dailyRate;
    }

    public Integer getId() { return id; }
    public Supplier getSupplier() { return supplier; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getPhone() { return phone; }
    public BigDecimal getDailyRate() { return dailyRate; }
}
