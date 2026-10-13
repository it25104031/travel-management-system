package com.travel.module2_visa.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A tour departure that groups several travellers' applications for batch processing and clearance. */
@Entity
@Table(name = "tour_departures")
public class TourDepartureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "departure_code", nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String tourName;

    // Country name as listed in supported_countries
    @Column(nullable = false)
    private String destinationCountry;

    @Column(nullable = false)
    private LocalDate departureDate;

    @Column(nullable = false)
    private LocalDate returnDate;

    @Column(name = "booking_gate_status")
    private String status = DepartureStatus.PENDING.name();

    // Last computed readiness (0-100), refreshed whenever the departure is viewed or its gate changes
    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal readinessPercentage = java.math.BigDecimal.ZERO;

    private String readyBy;
    private LocalDateTime readyAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "departure")
    @OrderBy("surname ASC")
    private List<VisaApplicationEntity> travellers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public DepartureStatus getStatusEnum() {
        try { return DepartureStatus.valueOf(status); } catch (Exception e) { return DepartureStatus.PENDING; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTourName() { return tourName; }
    public void setTourName(String tourName) { this.tourName = tourName; }

    public String getDestinationCountry() { return destinationCountry; }
    public void setDestinationCountry(String destinationCountry) { this.destinationCountry = destinationCountry; }

    public java.math.BigDecimal getReadinessPercentage() { return readinessPercentage; }
    public void setReadinessPercentage(java.math.BigDecimal readinessPercentage) { this.readinessPercentage = readinessPercentage; }

    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReadyBy() { return readyBy; }
    public void setReadyBy(String readyBy) { this.readyBy = readyBy; }

    public LocalDateTime getReadyAt() { return readyAt; }
    public void setReadyAt(LocalDateTime readyAt) { this.readyAt = readyAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public List<VisaApplicationEntity> getTravellers() { return travellers; }
    public void setTravellers(List<VisaApplicationEntity> travellers) { this.travellers = travellers; }
}
