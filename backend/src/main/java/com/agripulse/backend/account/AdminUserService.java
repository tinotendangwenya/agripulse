package com.agripulse.backend.account;

import com.agripulse.backend.account.AccountDtos.*;
import com.agripulse.backend.user.User;
import com.agripulse.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Admin tools: find a user, reset a password, switch an account off or on. Every change is written to the audit log. */
@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository users;
    private final UserAdminRepository search;
    private final ExpertApplicationRepository applications;
    private final AuditRepository auditRepo;
    private final AuditService audit;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public List<UserView> find(String q) {
        return search.search(q == null ? "" : q.trim(), PageRequest.of(0, 50)).stream().map(AdminUserService::view).toList();
    }

    @Transactional
    public UserView resetPassword(Long adminId, Long id, String temporaryPassword) {
        if (adminId.equals(id)) throw bad("Use Change password for your own account");
        User u = users.findById(id).orElseThrow(this::notFound);
        u.setPasswordHash(encoder.encode(temporaryPassword));
        u.setTokenVersion(u.getTokenVersion() + 1);
        audit.record(adminId, "RESET_PASSWORD", id, null);
        return view(u);
    }

    @Transactional
    public UserView setEnabled(Long adminId, Long id, boolean enabled) {
        if (adminId.equals(id)) throw bad("You cannot change your own account here");
        User u = users.findById(id).orElseThrow(this::notFound);
        if (enabled) {
            applications.findFirstByUserIdOrderByIdDesc(id)
                    .filter(a -> !"APPROVED".equals(a.getStatus()))
                    .ifPresent(a -> { throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "This person has an expert application that is " + a.getStatus() + ". Use Expert applications to decide it."); });
        }
        u.setEnabled(enabled);
        if (!enabled) u.setTokenVersion(u.getTokenVersion() + 1);
        audit.record(adminId, enabled ? "ENABLE_USER" : "DISABLE_USER", id, null);
        return view(u);
    }

    @Transactional(readOnly = true)
    public List<AuditView> audit() {
        return auditRepo.findTop100ByOrderByIdDesc().stream()
                .map(e -> new AuditView(e.getId(), e.getActorId(), e.getAction(), e.getTargetUserId(), e.getDetail(), e.getCreatedAt())).toList();
    }

    static UserView view(User u) {
        return new UserView(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole().getName(), u.isEnabled(), u.getConsentAt());
    }

    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
