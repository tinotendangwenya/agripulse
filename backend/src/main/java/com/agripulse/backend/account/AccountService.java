package com.agripulse.backend.account;

import com.agripulse.backend.account.AccountDtos.*;
import com.agripulse.backend.auth.AuthResponse;
import com.agripulse.backend.security.JwtService;
import com.agripulse.backend.security.LoginThrottle;
import com.agripulse.backend.user.User;
import com.agripulse.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** A user's own account. Changing the password logs the user out everywhere else (token version goes up). */
@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginThrottle throttle;
    private final AuditService audit;

    @Transactional
    public AuthResponse changePassword(Long userId, ChangePasswordRequest r) {
        String key = "pw:" + userId;
        throttle.check(key);
        User u = active(userId);
        if (!encoder.matches(r.currentPassword(), u.getPasswordHash())) {
            throttle.failure(key);
            throw bad("Your current password is not correct");
        }
        throttle.success(key);
        if (r.currentPassword().equals(r.newPassword()))
            throw bad("Choose a new password that is different from the current one");
        u.setPasswordHash(encoder.encode(r.newPassword()));
        u.setTokenVersion(u.getTokenVersion() + 1);
        audit.record(userId, "CHANGE_PASSWORD", userId, null);
        return new AuthResponse(jwt.generate(u), "Bearer", jwt.ttlSeconds(), u.getId(), u.getFullName(), u.getRole().getName());
    }

    /** Stops the account from logging in. The data is kept: erasing it is a manual job for now. */
    @Transactional
    public void deactivate(Long userId, String password) {
        String key = "pw:" + userId;
        throttle.check(key);
        User u = active(userId);
        if ("ADMIN".equals(u.getRole().getName()))
            throw bad("Admin accounts must be deactivated by another admin");
        if (!encoder.matches(password, u.getPasswordHash())) {
            throttle.failure(key);
            throw bad("Your password is not correct");
        }
        throttle.success(key);
        u.setEnabled(false);
        u.setTokenVersion(u.getTokenVersion() + 1);
        audit.record(userId, "SELF_DEACTIVATE", userId, null);
    }

    private User active(Long userId) {
        return users.findById(userId).filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in again"));
    }

    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
