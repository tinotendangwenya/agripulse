package com.agripulse.backend.supplies;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "supplier_profiles")
@Getter @Setter
public class SupplierProfile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Column(nullable = false, length = 60)
    private String province;

    @Column(length = 80)
    private String district;

    @Column(length = 255)
    private String address;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "offers_delivery", nullable = false)
    private boolean offersDelivery;

    @Column(nullable = false)
    private boolean verified;
}