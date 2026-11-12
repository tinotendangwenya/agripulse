package com.agripulse.backend.supplies;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "supply_products")
@Getter @Setter
public class SupplyProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id")
    private SupplierProfile supplier;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String brand;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "stock_qty", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockQty = BigDecimal.ZERO;

    @Column(name = "registration_no", length = 60)
    private String registrationNo;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
}