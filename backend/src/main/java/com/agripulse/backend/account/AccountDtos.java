package com.agripulse.backend.account;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class AccountDtos {
    private AccountDtos() {}

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 8, max = 72) String newPassword) {}

    public record DeactivateRequest(@NotBlank String password) {}

    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 72) String temporaryPassword) {}

    public record EnabledRequest(@NotNull Boolean enabled) {}

    public record NoteRequest(@Size(max = 300) String note) {}

    public record UserView(Long id, String fullName, String email, String phone, String role, boolean enabled,
                           LocalDateTime consentAt) {}

    public record AuditView(Long id, Long actorId, String action, Long targetUserId, String detail,
                            LocalDateTime createdAt) {}

    public record ApplyRequest(@NotBlank @Size(max = 150) String fullName,
                               @Email @Size(max = 255) String email,
                               @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Phone must be 9-15 digits, optionally starting with +") String phone,
                               @NotBlank @Size(min = 8, max = 72) String password,
                               @NotBlank @Size(max = 200) String qualification,
                               @Size(max = 150) String organisation,
                               @Size(max = 80) String registrationNumber,
                               @NotBlank String province,
                               @NotBlank @Size(max = 150) String speciality,
                               @Size(max = 1000) String statement,
                               Boolean acceptedPrivacy) {}

    public record ApplyResult(String message) {}

    public record ApplicationView(Long id, Long userId, String fullName, String email, String phone,
                                  String qualification, String organisation, String registrationNumber,
                                  String province, String speciality, String statement, String status,
                                  String reviewNote, LocalDateTime createdAt) {}
}
