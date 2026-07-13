package ru.formatkoda.polybank.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffUserRegistrationDto(
        @NotBlank String login,
        @NotBlank String name,
        @NotBlank String lastName,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String roleName
) {
}
