package com.agripulse.backend.ai;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_predictions")
@Getter @Setter
public class AiPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "farm_id")
    private Long farmId;

    @Column(nullable = false, length = 40)
    private String crop;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "top_label", length = 150)
    private String topLabel;

    @Column(name = "top_confidence", precision = 5, scale = 3)
    private BigDecimal topConfidence;

    @Column(nullable = false, length = 150)
    private String model;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
