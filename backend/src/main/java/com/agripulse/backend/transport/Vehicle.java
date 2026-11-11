package com.agripulse.backend.transport;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Getter @Setter
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "vehicle_type", nullable = false, length = 30)
    private String vehicleType;

    @Column(nullable = false, length = 150)
    private String description;

    @Column(name = "capacity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal capacityKg;

    @Column(name = "cargo_types", nullable = false, length = 60)
    private String cargoTypes;

    @Column(name = "base_province", nullable = false, length = 60)
    private String baseProvince;

    @Column(name = "base_district", length = 80)
    private String baseDistrict;

    @Column(name = "rate_per_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerKm;

    @Column(name = "min_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal minCharge = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
