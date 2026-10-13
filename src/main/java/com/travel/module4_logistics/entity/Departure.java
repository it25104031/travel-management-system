package com.travel.module4_logistics.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "departure")
public class Departure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "departure_id")
    private Integer id;
    @Column(nullable = false, unique = true, length = 20)
    private String reference;
    @Column(name = "package_name", nullable = false, length = 100)
    private String packageName;
    @Column(nullable = false, length = 100)
    private String destination;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    protected Departure() { }
    public Departure(String reference, String packageName, String destination, LocalDate startDate, LocalDate endDate) {
        this.reference = reference; this.packageName = packageName; this.destination = destination;
        this.startDate = startDate; this.endDate = endDate;
    }
    public void updateDetails(String reference, String packageName, String destination, LocalDate startDate, LocalDate endDate) {
        this.reference = reference; this.packageName = packageName; this.destination = destination;
        this.startDate = startDate; this.endDate = endDate;
    }

    public Integer getId() { return id; }
    public String getReference() { return reference; }
    public String getPackageName() { return packageName; }
    public String getDestination() { return destination; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
}
