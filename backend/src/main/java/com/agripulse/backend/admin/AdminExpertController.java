package com.agripulse.backend.admin;

import com.agripulse.backend.user.RoleRepository;
import com.agripulse.backend.user.User;
import com.agripulse.backend.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Admins create expert accounts. Experts cannot self-register. ADMIN only (via /api/admin/** in SecurityConfig). */
@RestController
@RequestMapping("/api/admin/experts")
@RequiredArgsConstructor
public class AdminExpertController {
    public record CreateExpert(@NotBlank @Size(max = 150) String fullName,
                               @Email @Size(max = 255) String email,
                               @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Phone must be 9-15 digits") String phone,
                               @NotBlank @Size(min = 8, max = 72) String password) {}

    public record ExpertView(Long id, String fullName, String email, String phone) {}

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpertView create(@Valid @RequestBody CreateExpert r) {
        String email = (r.email() == null || r.email().isBlank()) ? null : r.email().trim().toLowerCase();
        String phone = (r.phone() == null || r.phone().isBlank()) ? null : r.phone().trim();
        if (email == null && phone == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide an email or a phone number");
        if (email != null && users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        if (phone != null && users.existsByPhone(phone)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already registered");

        User u = new User();
        u.setFullName(r.fullName().trim());
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole(roles.findByName("EXPERT").orElseThrow());
        users.save(u);
        return new ExpertView(u.getId(), u.getFullName(), u.getEmail(), u.getPhone());
    }
}
