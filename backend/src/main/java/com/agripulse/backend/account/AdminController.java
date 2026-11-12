package com.agripulse.backend.account;

import com.agripulse.backend.account.AccountDtos.*;
import com.agripulse.backend.notifications.NotificationCleanup;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Admin only. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final AdminUserService users;
    private final ExpertApplicationService applications;
    private final NotificationCleanup cleanup;

    private static Long uid(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }

    @GetMapping("/users")
    public List<UserView> find(@RequestParam(required = false) String q) {
        return users.find(q);
    }

    @PostMapping("/users/{id}/reset-password")
    public UserView resetPassword(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody ResetPasswordRequest r) {
        return users.resetPassword(uid(jwt), id, r.temporaryPassword());
    }

    @PostMapping("/users/{id}/enabled")
    public UserView enabled(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody EnabledRequest r) {
        return users.setEnabled(uid(jwt), id, r.enabled());
    }

    @GetMapping("/audit")
    public List<AuditView> audit() {
        return users.audit();
    }

    @GetMapping("/applications")
    public List<ApplicationView> applications(@RequestParam(required = false) String status) {
        return applications.list(status);
    }

    @PostMapping("/applications/{id}/approve")
    public ApplicationView approve(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return applications.approve(uid(jwt), id);
    }

    @PostMapping("/applications/{id}/reject")
    public ApplicationView reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody(required = false) NoteRequest r) {
        return applications.reject(uid(jwt), id, r == null ? null : r.note());
    }

    @PostMapping("/maintenance/purge-notifications")
    public Map<String, Integer> purge() {
        return Map.of("deleted", cleanup.purge());
    }
}
