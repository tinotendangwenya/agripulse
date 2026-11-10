package com.agripulse.backend.qa;

import java.time.LocalDateTime;
import java.util.List;

public final class QaDtos {
    private QaDtos() {}

    public record QuestionSummary(Long id, String title, String subject, String topic, String status,
                                  String farmerName, String farmName, String province,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record MessageView(Long id, String authorName, String authorRole, String body,
                              boolean hasImage, LocalDateTime createdAt) {}

    public record ThreadView(QuestionSummary question, List<MessageView> messages) {}

    public record ImageData(byte[] bytes, String type) {}
}
