package com.travel.module4_logistics.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "supplier")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "contract_start", nullable = false)
    private LocalDate contractStart;

    @Column(name = "contract_end", nullable = false)
    private LocalDate contractEnd;

    @Column(name = "room_capacity")
    private Integer roomCapacity;

    protected Supplier() { }

    public Supplier(String name, String type, String phone, LocalDate start, LocalDate end, Integer rooms) {
        this.name = name;
        this.type = type;
        this.contactPhone = phone;
        this.contractStart = start;
        this.contractEnd = end;
        this.roomCapacity = rooms;
    }

    public void updateDetails(String name, String type, String phone,
                              LocalDate start, LocalDate end, Integer rooms) {
        this.name = name;
        this.type = type;
        this.contactPhone = phone;
        this.contractStart = start;
        this.contractEnd = end;
        this.roomCapacity = rooms;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getContactPhone() { return contactPhone; }
    public LocalDate getContractStart() { return contractStart; }
    public LocalDate getContractEnd() { return contractEnd; }
    public Integer getRoomCapacity() { return roomCapacity; }
}
