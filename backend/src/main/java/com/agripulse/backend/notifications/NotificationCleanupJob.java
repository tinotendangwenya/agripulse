package com.agripulse.backend.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Runs the clean-up every night at 03:30 Harare time. */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class NotificationCleanupJob {
    private final NotificationCleanup cleanup;

    @Scheduled(cron = "0 30 3 * * *", zone = "Africa/Harare")
    public void nightly() {
        cleanup.purge();
    }
}
