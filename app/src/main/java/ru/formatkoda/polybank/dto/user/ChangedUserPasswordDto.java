package ru.formatkoda.polybank.dto.user;

import jakarta.validation.constraints.NotBlank;

public record ChangedUserPasswordDto(
        @NotBlank String login,
        @NotBlank String oldPassword,
        @NotBlank String newPassword
) {
}
