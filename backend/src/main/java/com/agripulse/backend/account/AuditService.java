package com.agripulse.backend.account;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records who did what to whose account. Runs inside the caller's transaction, so a failed action leaves no record. */
@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditRepository repo;

    @Transactional
    public void record(Long actorId, String action, Long targetUserId, String detail) {
        AuditEntry e = new AuditEntry();
        e.setActorId(actorId);
        e.setAction(action);
        e.setTargetUserId(targetUserId);
        e.setDetail(detail == null ? null : (detail.length() > 300 ? detail.substring(0, 300) : detail));
        repo.save(e);
    }
}
