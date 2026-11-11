package com.agripulse.backend.equipment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipment")
@Getter @Setter
public class Equipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 60)
    private String province;

    @Column(length = 80)
    private String district;

    @Column(name = "daily_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyRate;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "operator_included", nullable = false)
    private boolean operatorIncluded;

    @Column(name = "fuel_included", nullable = false)
    private boolean fuelIncluded;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deposit = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
