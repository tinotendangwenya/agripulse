package com.agripulse.backend.prices;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_quotes")
@Getter @Setter
public class PriceQuote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submitter_id", nullable = false)
    private Long submitterId;

    @Column(nullable = false, length = 80)
    private String commodity;

    @Column(nullable = false, length = 100)
    private String market;

    @Column(nullable = false, length = 60)
    private String province;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(length = 300)
    private String note;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 300)
    private String reviewNote;

    @Column(name = "market_price_id")
    private Long marketPriceId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
