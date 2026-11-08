package com.agripulse.backend.auth;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 150) String fullName,
        @Email @Size(max = 255) String email,
        @Pattern(regexp = "^\\+?[0-9]{9,15}$",
                 message = "Phone must be 9-15 digits, optionally starting with +") String phone,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String role) {}
