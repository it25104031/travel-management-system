package com.travel.module2_visa.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** A visa category offered for a destination (e.g. India eTOURIST VISA) with its fee and processing time. */
@Entity
@Table(name = "visa_types")
public class VisaTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "country_id")
    private CountryEntity country;

    @Column(name = "visa_name", nullable = false)
    private String name;

    // TOURIST_EVISA, PILGRIM, GROUP, TRANSIT, SPECIAL_PERMIT, BUSINESS, MEDICAL ...
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    private String currency = "USD";

    @Column(name = "standard_processing_days", nullable = false)
    private Integer processingDays = 5;

    private Boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CountryEntity getCountry() { return country; }
    public void setCountry(CountryEntity country) { this.country = country; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getProcessingDays() { return processingDays; }
    public void setProcessingDays(Integer processingDays) { this.processingDays = processingDays; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
