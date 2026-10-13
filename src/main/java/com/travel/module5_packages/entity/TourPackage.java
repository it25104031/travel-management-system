package com.travel.module5_packages.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="tour_package")
public class TourPackage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="package_id")
    private Integer packageId;
    @Column(name="package_name",nullable=false,length=150) private String packageName;
    @Column(length=1000) private String description;
    @Column(nullable=false) private Integer duration;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal price;
    @Column(nullable=false,length=30) private String status;

    // Shown on the public website (all optional)
    @Column(length=100) private String destination;
    @Column(length=40) private String category;
    @Column(name="image_url",length=500) private String imageUrl;

    @OneToMany(mappedBy="tourPackage",cascade=CascadeType.ALL,orphanRemoval=true)
    @OrderBy("dayNumber ASC")
    private List<Itinerary> itineraries=new ArrayList<>();

    public TourPackage(){}
    public Integer getPackageId(){return packageId;}
    public String getPackageName(){return packageName;}
    public void setPackageName(String v){packageName=v;}
    public String getDescription(){return description;}
    public void setDescription(String v){description=v;}
    public Integer getDuration(){return duration;}
    public void setDuration(Integer v){duration=v;}
    public BigDecimal getPrice(){return price;}
    public void setPrice(BigDecimal v){price=v;}
    public String getStatus(){return status;}
    public void setStatus(String v){status=v;}
    public String getDestination(){return destination;}
    public void setDestination(String v){destination=v;}
    public String getCategory(){return category;}
    public void setCategory(String v){category=v;}
    public String getImageUrl(){return imageUrl;}
    public void setImageUrl(String v){imageUrl=v;}
    public List<Itinerary> getItineraries(){return itineraries;}

    public boolean isAvailable(){return "AVAILABLE".equals(status);}

    /** Number of nights for display ("5 days / 4 nights"). */
    public int getNights(){return duration==null||duration<1?0:duration-1;}

    /** A stable 0-5 index used to pick the card colour on the website when there is no image. */
    public int getThemeIndex(){
        String key = category!=null&&!category.isBlank()?category:(packageName==null?"":packageName);
        return Math.floorMod(key.toLowerCase().hashCode(),6);
    }
}
