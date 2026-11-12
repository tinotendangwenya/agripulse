package com.agripulse.backend.auth;

import com.agripulse.backend.account.AccountDtos.ApplyRequest;
import com.agripulse.backend.account.AccountDtos.ApplyResult;
import com.agripulse.backend.account.ExpertApplicationService;
import com.agripulse.backend.security.LoginThrottle;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    private final LoginThrottle throttle;
    private final ExpertApplicationService applications;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        String key = "login:" + request.identifier().trim().toLowerCase();
        throttle.check(key);
        try {
            AuthResponse r = auth.login(request);
            throttle.success(key);
            return r;
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == 401) {
                // A correct password on an expert application that is not approved yet is not a failed guess
                String pending = applications.pendingMessage(request.identifier(), request.password());
                if (pending != null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, pending);
                throttle.failure(key);
            }
            throw e;
        }
    }

    /** Public. Creates a DISABLED expert account plus an application for an admin to review. Limited per IP address. */
    @PostMapping("/apply-expert")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplyResult applyExpert(@Valid @RequestBody ApplyRequest request, HttpServletRequest http) {
        String key = "apply:" + http.getRemoteAddr();
        throttle.check(key);
        throttle.failure(key);          // every attempt counts, so at most 5 per 15 minutes per address
        return applications.apply(request);
    }
}
