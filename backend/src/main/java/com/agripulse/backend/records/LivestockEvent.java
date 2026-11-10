package com.agripulse.backend.records;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "livestock_events")
@Getter @Setter
public class LivestockEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farm_id", nullable = false)
    private Long farmId;

    @Column(name = "animal_type", nullable = false, length = 20)
    private String animalType;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "head_count", nullable = false)
    private Integer headCount;

    @Column(length = 300)
    private String note;
}
