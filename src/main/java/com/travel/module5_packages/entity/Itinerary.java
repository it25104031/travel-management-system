package com.travel.module5_packages.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="itinerary",
 uniqueConstraints=@UniqueConstraint(name="uq_package_day",columnNames={"package_id","day_number"}))
public class Itinerary {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="itinerary_id") private Integer itineraryId;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="package_id",nullable=false) private TourPackage tourPackage;
    @Column(name="day_number",nullable=false) private Integer dayNumber;
    @Column(length=100) private String destination;
    @Column(length=1000) private String activities;
    @Column(length=255) private String accommodation;
    @Column(length=255) private String transport;

    public Itinerary(){}
    public Integer getItineraryId(){return itineraryId;}
    @JsonIgnore
    public TourPackage getTourPackage(){return tourPackage;}
    public void setTourPackage(TourPackage v){tourPackage=v;}
    public Integer getDayNumber(){return dayNumber;}
    public void setDayNumber(Integer v){dayNumber=v;}
    public String getDestination(){return destination;}
    public void setDestination(String v){destination=v;}
    public String getActivities(){return activities;}
    public void setActivities(String v){activities=v;}
    public String getAccommodation(){return accommodation;}
    public void setAccommodation(String v){accommodation=v;}
    public String getTransport(){return transport;}
    public void setTransport(String v){transport=v;}
}