package com.agripulse.backend.auth;

import com.agripulse.backend.security.JwtService;
import com.agripulse.backend.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    // ADMIN and EXPERT accounts are never self-registered
    private static final Set<String> SELF_REGISTER = Set.of("FARMER", "BUYER", "SUPPLIER");

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        String email = blankToNull(r.email());
        String phone = blankToNull(r.phone());
        if (email == null && phone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide an email or a phone number");
        }
        String roleName = r.role().trim().toUpperCase();
        if (!SELF_REGISTER.contains(roleName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Role must be one of " + SELF_REGISTER);
        }
        if (email != null) {
            email = email.toLowerCase();
            if (users.existsByEmailIgnoreCase(email)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
            }
        }
        if (phone != null && users.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already registered");
        }

        User u = new User();
        u.setFullName(r.fullName().trim());
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole(roles.findByName(roleName).orElseThrow());
        users.save(u);
        return toResponse(u);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        String id = r.identifier().trim();
        var found = id.contains("@") ? users.findByEmailIgnoreCase(id) : users.findByPhone(id);
        User u = found
                .filter(User::isEnabled)
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return toResponse(u);
    }

    private AuthResponse toResponse(User u) {
        return new AuthResponse(jwt.generate(u), "Bearer", jwt.ttlSeconds(),
                u.getId(), u.getFullName(), u.getRole().getName());
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
