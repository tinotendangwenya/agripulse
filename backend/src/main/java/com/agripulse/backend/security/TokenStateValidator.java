package com.agripulse.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Runs on every request. A token stops working as soon as its account is disabled or its token version changes
 * (password change, admin reset). Tokens issued before this existed have no version and count as version 0.
 */
@Component
@RequiredArgsConstructor
public class TokenStateValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID = new OAuth2Error("invalid_token", "The session is no longer valid", null);

    private final TokenStateRepository states;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Long id;
        try {
            id = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            return OAuth2TokenValidatorResult.failure(INVALID);
        }
        Optional<Integer> current = states.activeTokenVersion(id);
        if (current.isEmpty()) return OAuth2TokenValidatorResult.failure(INVALID);
        Object claim = jwt.getClaims().get("tv");
        int tokenVersion = claim instanceof Number n ? n.intValue() : 0;
        return tokenVersion == current.get() ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(INVALID);
    }
}
