package com.agripulse.backend.account;

import com.agripulse.backend.account.AccountDtos.*;
import com.agripulse.backend.auth.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Any logged-in user, for their own account only. The user id comes from the token. */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService service;

    @PostMapping("/change-password")
    public AuthResponse changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest r) {
        return service.changePassword(Long.valueOf(jwt.getSubject()), r);
    }

    @PostMapping("/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeactivateRequest r) {
        service.deactivate(Long.valueOf(jwt.getSubject()), r.password());
    }
}
