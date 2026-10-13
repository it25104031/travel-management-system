package com.travel.module4_logistics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transport")
public class Transport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transport_id")
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_id", nullable = false)
    private Departure departure;

    @Column(nullable = false, length = 20)
    private String mode;

    @Column(name = "vehicle_reference", nullable = false, length = 60)
    private String vehicleReference;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(nullable = false)
    private Integer seats;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;

    protected Transport() { }

    public Transport(Supplier supplier, Departure departure, String mode, String reference, LocalDate date, Integer seats, BigDecimal cost) {
        this.supplier = supplier;
        this.departure = departure;
        this.mode = mode;
        this.vehicleReference = reference;
        this.serviceDate = date;
        this.seats = seats;
        this.cost = cost;
    }

    public void updateDetails(Supplier supplier, Departure departure, String mode, String reference, LocalDate date, Integer seats, BigDecimal cost) {
        this.supplier = supplier;
        this.departure = departure;
        this.mode = mode;
        this.vehicleReference = reference;
        this.serviceDate = date;
        this.seats = seats;
        this.cost = cost;
    }

    public Integer getId() { return id; }
    public Supplier getSupplier() { return supplier; }
    public Departure getDeparture() { return departure; }
    public String getMode() { return mode; }
    public String getVehicleReference() { return vehicleReference; }
    public LocalDate getServiceDate() { return serviceDate; }
    public Integer getSeats() { return seats; }
    public BigDecimal getCost() { return cost; }
}
