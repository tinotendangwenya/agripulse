package com.agripulse.backend.qa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "question_messages")
@Getter @Setter
public class QuestionMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_role", nullable = false, length = 20)
    private String authorRole;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "image_file", length = 80)
    private String imageFile;

    @Column(name = "image_type", length = 30)
    private String imageType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
