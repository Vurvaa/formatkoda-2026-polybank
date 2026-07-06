package ru.formatkoda.polybank.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginDto(
        @NotBlank String login,
        @NotBlank @Size(min = 8) String password
) {
}
