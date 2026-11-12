package com.agripulse.backend.notifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Deletes alerts that were read more than 30 days ago, and any alert older than 90 days. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanup {
    private final NotificationCleanupRepository repo;

    @Transactional
    public int purge() {
        LocalDateTime now = LocalDateTime.now();
        int n = repo.purge(now.minusDays(30), now.minusDays(90));
        log.info("Deleted {} old notifications", n);
        return n;
    }
}
