package com.agripulse.backend.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/** In-app alerts. Other services call send() inside their own transaction, so a rolled-back action sends nothing. */
@Service
@RequiredArgsConstructor
public class NotificationService {
    public record NotificationView(Long id, String type, String title, String body, String link,
                                   boolean read, LocalDateTime createdAt) {}
    public record NotificationList(long unread, List<NotificationView> items) {}
    public record Unread(long unread) {}

    private final NotificationRepository repo;

    @Transactional
    public void send(Long userId, String type, String title, String body, String link) {
        if (userId == null) return;
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(cut(type, 20));
        n.setTitle(cut(title, 100));
        n.setBody(cut(body, 300));
        n.setLink(cut(link, 40));
        repo.save(n);
    }

    @Transactional(readOnly = true)
    public NotificationList list(Long userId) {
        List<NotificationView> items = repo.findTop50ByUserIdOrderByIdDesc(userId).stream()
                .map(n -> new NotificationView(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(),
                        n.getReadAt() != null, n.getCreatedAt())).toList();
        return new NotificationList(repo.countByUserIdAndReadAtIsNull(userId), items);
    }

    @Transactional(readOnly = true)
    public Unread unread(Long userId) {
        return new Unread(repo.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        Notification n = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        if (n.getReadAt() == null) n.setReadAt(LocalDateTime.now());
    }

    @Transactional
    public void markAllRead(Long userId) {
        repo.markAllRead(userId, LocalDateTime.now());
    }

    private static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
