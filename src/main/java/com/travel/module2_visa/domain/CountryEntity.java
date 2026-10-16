package com.travel.module2_visa.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "supported_countries")
public class CountryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String countryName;

    @Column(nullable = false, unique = true)
    private String countryCode;

    @Column(nullable = false)
    private String formRoute;

    private Boolean isActive = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCountryName() { return countryName; }
    public void setCountryName(String countryName) { this.countryName = countryName; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getFormRoute() { return formRoute; }
    public void setFormRoute(String formRoute) { this.formRoute = formRoute; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
