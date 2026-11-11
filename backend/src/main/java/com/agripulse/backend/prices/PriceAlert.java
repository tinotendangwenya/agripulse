package com.agripulse.backend.prices;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_alerts")
@Getter @Setter
public class PriceAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 80)
    private String commodity;

    @Column(length = 100)
    private String market;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, length = 5)
    private String direction;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal threshold;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "triggered_price", precision = 12, scale = 2)
    private BigDecimal triggeredPrice;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
