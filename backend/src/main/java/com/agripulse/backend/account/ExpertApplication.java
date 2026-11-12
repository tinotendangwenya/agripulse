package com.agripulse.backend.account;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "expert_applications")
@Getter @Setter
public class ExpertApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String qualification;

    @Column(length = 150)
    private String organisation;

    @Column(name = "registration_number", length = 80)
    private String registrationNumber;

    @Column(nullable = false, length = 60)
    private String province;

    @Column(nullable = false, length = 150)
    private String speciality;

    @Column(length = 1000)
    private String statement;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 300)
    private String reviewNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
