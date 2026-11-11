package com.agripulse.backend.transport;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transport_requests")
@Getter @Setter
public class TransportRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "cargo_type", nullable = false, length = 20)
    private String cargoType;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "pickup_province", nullable = false, length = 60)
    private String pickupProvince;

    @Column(name = "pickup_district", length = 80)
    private String pickupDistrict;

    @Column(name = "delivery_province", nullable = false, length = 60)
    private String deliveryProvince;

    @Column(name = "delivery_district", length = 80)
    private String deliveryDistrict;

    @Column(name = "needed_by")
    private LocalDate neededBy;

    @Column(length = 500)
    private String note;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "accepted_quote_id")
    private Long acceptedQuoteId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
