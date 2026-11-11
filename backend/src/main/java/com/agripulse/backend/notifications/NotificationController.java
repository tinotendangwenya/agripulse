package com.agripulse.backend.notifications;

import com.agripulse.backend.notifications.NotificationService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Any logged-in user sees only their own alerts. The user id comes from the token. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    private static Long uid(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    @GetMapping
    public NotificationList list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(uid(jwt));
    }

    @GetMapping("/unread-count")
    public Unread unread(@AuthenticationPrincipal Jwt jwt) {
        return service.unread(uid(jwt));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.markRead(uid(jwt), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(@AuthenticationPrincipal Jwt jwt) {
        service.markAllRead(uid(jwt));
    }
}
