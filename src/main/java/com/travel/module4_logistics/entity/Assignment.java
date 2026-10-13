package com.travel.module4_logistics.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "assignment")
public class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_id", nullable = false)
    private Departure departure;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guide;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;

    protected Assignment() { }

    public Assignment(Departure departure, Guide guide, LocalDate start, LocalDate end, BigDecimal cost) {
        this.departure = departure;
        this.guide = guide;
        this.startDate = start;
        this.endDate = end;
        this.cost = cost;
    }

    public void updateDetails(Departure departure, Guide guide, LocalDate start, LocalDate end, BigDecimal cost) {
        this.departure = departure;
        this.guide = guide;
        this.startDate = start;
        this.endDate = end;
        this.cost = cost;
    }

    public Integer getId() { return id; }
    public Departure getDeparture() { return departure; }
    public Guide getGuide() { return guide; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getCost() { return cost; }
}
