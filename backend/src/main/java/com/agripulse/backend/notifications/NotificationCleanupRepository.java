package com.agripulse.backend.notifications;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationCleanupRepository extends Repository<Notification, Long> {
    @Modifying
    @Query("delete from Notification n where (n.readAt is not null and n.readAt < :readBefore) or n.createdAt < :anyBefore")
    int purge(@Param("readBefore") LocalDateTime readBefore, @Param("anyBefore") LocalDateTime anyBefore);
}
