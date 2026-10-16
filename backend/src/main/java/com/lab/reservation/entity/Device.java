package com.lab.reservation.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "lab_device")
public class Device {

    public enum Status { AVAILABLE, MAINTENANCE, DISABLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String model;         // 型号规格

    @Column(length = 100)
    private String location;      // 所在实验室

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.AVAILABLE;

    @Column(length = 500)
    private String description;
}
