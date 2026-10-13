package com.travel.module4_logistics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "hotel_booking")
public class HotelBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hotel_booking_id")
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_id", nullable = false)
    private Departure departure;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private Integer rooms;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;

    protected HotelBooking() { }

    public HotelBooking(Supplier supplier, Departure departure, LocalDate checkIn, LocalDate checkOut, Integer rooms, BigDecimal cost) {
        this.supplier = supplier;
        this.departure = departure;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.rooms = rooms;
        this.cost = cost;
    }

    public void updateDetails(Supplier supplier, Departure departure, LocalDate checkIn, LocalDate checkOut, Integer rooms, BigDecimal cost) {
        this.supplier = supplier;
        this.departure = departure;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.rooms = rooms;
        this.cost = cost;
    }

    public Integer getId() { return id; }
    public Supplier getSupplier() { return supplier; }
    public Departure getDeparture() { return departure; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public Integer getRooms() { return rooms; }
    public BigDecimal getCost() { return cost; }
}
