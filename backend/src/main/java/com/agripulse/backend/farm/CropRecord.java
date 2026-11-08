package com.agripulse.backend.farm;

/**
 * Entities and repositories (database mapping and queries)
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "crop_records")
@Getter @Setter
public class CropRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @Column(name = "crop_name", nullable = false, length = 80)
    private String cropName;

    @Column(length = 80)
    private String variety;

    @Column(name = "area_hectares", precision = 10, scale = 2)
    private BigDecimal areaHectares;

    @Column(name = "planting_date")
    private LocalDate plantingDate;

    @Column(name = "expected_harvest_date")
    private LocalDate expectedHarvestDate;

    @Column(nullable = false, length = 30)
    private String status = "PLANNED";

    @Column(columnDefinition = "TEXT")
    private String notes;
}
